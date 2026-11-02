package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryAllocation;
import com.kittyp.clinic.entity.ClinicInventoryBatch;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryAllocationRepository;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.entity.ClinicInventoryReservation;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryMovementSource;
import com.kittyp.clinic.enums.InventoryReservationStatus;
import com.kittyp.clinic.repository.ClinicInventoryReservationRepository;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.common.constants.AppConstant;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.dto.TreatmentLineItemDto;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.doctor.enums.TreatmentInvoiceItemType;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicInventoryConsumptionService {

    private final ClinicInventoryItemRepository inventoryRepository;
    private final ClinicInventoryMovementRepository movementRepository;
    private final ClinicInventoryReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final ClinicInventoryStockSupport stock;
    private final ClinicInventoryAllocationRepository allocationRepository;

    /** Bind stocked lines to this clinic's catalog. Does not change quantities. */
    public void prepareLines(Clinic clinic, List<TreatmentLineItemDto> items) {
        if (items == null) {
            return;
        }
        for (TreatmentLineItemDto line : items) {
            if (clinic == null || !isStocked(line.getItemType())) {
                line.setInventoryItemUuid(null);
                continue;
            }
            String itemUuid = blankToNull(line.getInventoryItemUuid());
            if (itemUuid == null) {
                throw new CustomException("Choose an inventory item for " + line.getItemType().name().toLowerCase()
                        + " lines", HttpStatus.BAD_REQUEST);
            }
            ClinicInventoryItem item = inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(itemUuid, clinic.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("clinic inventory item", "uuid", itemUuid));
            requireCategory(line.getItemType(), item);
            requireWholeUnits(line.getQuantity(), item.getName());
            line.setInventoryItemUuid(item.getUuid());
            line.setDescription(item.getName());
            line.setUnit(item.getUnit());
        }
    }

    /** Drop this draft's active holds so a later reserve can take the edited lines. */
    @Transactional
    public void releaseActiveHolds(ConsultationInvoice invoice) {
        if (invoice.getUuid() == null) {
            return;
        }
        List<ClinicInventoryReservation> active = reservationRepository.findByInvoiceUuidAndStatus(invoice.getUuid(),
                InventoryReservationStatus.ACTIVE);
        if (active == null) {
            return;
        }
        for (ClinicInventoryReservation reservation : active) {
            releaseReservation(reservation);
        }
    }

    /**
     * Hold eligible (unexpired, reviewed) stock for a draft, FEFO across batches. Does not reduce on-hand
     * quantity. Lines that cannot be fully covered by eligible stock are left unreserved.
     */
    @Transactional
    public void reserveForDraft(ConsultationInvoice invoice) {
        if (invoice.getClinic() == null || invoice.getUuid() == null) {
            return;
        }
        releaseExpiredReservations();
        if (alreadyConsumed(invoice) || reservationRepository.existsByInvoiceUuidAndStatusAndExpiresAtAfter(
                invoice.getUuid(), InventoryReservationStatus.ACTIVE, LocalDateTime.now())) {
            return;
        }
        User actor = currentActor();
        int reservationMinutes = reservationMinutes();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(reservationMinutes);
        LocalDate today = LocalDate.now();
        for (TreatmentLineItemDto line : stockedLines(invoice)) {
            ClinicInventoryItem item = lockItem(invoice, line.getInventoryItemUuid());
            requireCategory(line.getItemType(), item);
            int units = requireWholeUnits(line.getQuantity(), item.getName());
            List<ClinicInventoryBatch> batches = stock.batches(item);
            stock.ensureLegacyBatch(item, batches);
            List<ClinicInventoryStockSupport.Slot> slots = stock.slots(item, batches, today);
            if (units > slots.stream().mapToInt(ClinicInventoryStockSupport.Slot::available).sum()) {
                continue;
            }
            int left = units;
            for (ClinicInventoryStockSupport.Slot slot : slots) {
                if (left == 0) {
                    break;
                }
                int take = Math.min(left, slot.available());
                left -= take;
                ClinicInventoryReservation reservation = ClinicInventoryReservation.builder()
                        .clinic(invoice.getClinic())
                        .itemUuid(item.getUuid())
                        .invoiceUuid(invoice.getUuid())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .quantity(take)
                        .expiresAt(expiresAt)
                        .status(InventoryReservationStatus.ACTIVE)
                        .batch(slot.batch())
                        .build();
                reservation.setIsActive(true);
                reservationRepository.save(reservation);
                stock.record(invoice.getClinic(), item, actor, InventoryMovementAction.RESERVE, -take,
                        ClinicInventoryStockSupport.qty(slot.batch()),
                        "Reserved " + take + " for " + reservationMinutes + " minutes (batch "
                                + slot.batch().getBatchNumber() + ")",
                        InventoryMovementSource.INVOICE, invoice.getUuid(), invoice.getInvoiceNumber(), slot.batch());
            }
        }
    }

    /**
     * PDF generation and mark paid. Allocates FEFO from eligible batches (expired/unreviewed stock is never
     * consumed), decrements batch quantities, records allocations and per-batch movements, and re-syncs the item
     * aggregate. The invoice's own active holds are settled first; every other invoice's holds are respected.
     */
    @Transactional
    public void consumeForIssue(ConsultationInvoice invoice) {
        if (invoice.getClinic() == null || invoice.getUuid() == null || alreadyConsumed(invoice)) {
            return;
        }
        releaseExpiredReservations();
        User actor = currentActor();
        LocalDate today = LocalDate.now();
        List<TreatmentLineItemDto> all = readItems(invoice.getLineItems());
        Map<String, List<Integer>> linesByItem = new TreeMap<>();
        for (int i = 0; i < all.size(); i++) {
            TreatmentLineItemDto line = all.get(i);
            if (isStocked(line.getItemType()) && blankToNull(line.getInventoryItemUuid()) != null) {
                linesByItem.computeIfAbsent(line.getInventoryItemUuid().trim(), k -> new ArrayList<>()).add(i);
            }
        }
        for (Map.Entry<String, List<Integer>> entry : linesByItem.entrySet()) { // sorted: stable lock order
            ClinicInventoryItem item = lockItem(invoice, entry.getKey());
            List<ClinicInventoryBatch> batches = stock.batches(item);
            stock.ensureLegacyBatch(item, batches);
            List<ClinicInventoryReservation> holds = reservationRepository.findByInvoiceUuidAndItemUuidAndStatus(
                    invoice.getUuid(), item.getUuid(), InventoryReservationStatus.ACTIVE);
            if (holds != null) {
                for (ClinicInventoryReservation hold : holds) {
                    boolean usable = hold.getBatch() == null
                            || ClinicInventoryStockSupport.eligible(hold.getBatch(), today);
                    hold.setStatus(usable ? InventoryReservationStatus.CONSUMED : InventoryReservationStatus.RELEASED);
                    reservationRepository.save(hold);
                }
            }
            for (int index : entry.getValue()) {
                TreatmentLineItemDto line = all.get(index);
                requireCategory(line.getItemType(), item);
                int units = requireWholeUnits(line.getQuantity(), item.getName());
                List<ClinicInventoryStockSupport.Slot> slots = stock.slots(item, batches, today);
                int free = slots.stream().mapToInt(ClinicInventoryStockSupport.Slot::available).sum();
                if (units > free) {
                    ClinicInventoryStockSupport.ItemStock s = ClinicInventoryStockSupport.summarize(item, batches,
                            today, Map.of(), 0);
                    String unit = item.getUnit() == null || item.getUnit().isBlank() ? "units" : item.getUnit();
                    throw new CustomException("Only " + free + " eligible " + item.getName()
                            + " available (not expired or awaiting expiry review)."
                            + (s.expired() + s.unreviewed() > 0
                                    ? " " + s.expired() + " expired and " + s.unreviewed() + " unreviewed " + unit
                                            + " cannot be billed."
                                    : "")
                            + " Add stock or review expiry in inventory before billing " + units + ".",
                            HttpStatus.CONFLICT);
                }
                int left = units;
                for (ClinicInventoryStockSupport.Slot slot : slots) {
                    if (left == 0) {
                        break;
                    }
                    int take = Math.min(left, slot.available());
                    left -= take;
                    ClinicInventoryBatch batch = slot.batch();
                    batch.setQuantityOnHand(ClinicInventoryStockSupport.qty(batch) - take);
                    stock.saveBatch(batch);
                    ClinicInventoryAllocation allocation = new ClinicInventoryAllocation();
                    allocation.setClinic(invoice.getClinic());
                    allocation.setInvoiceUuid(invoice.getUuid());
                    allocation.setInvoiceLineIndex(index);
                    allocation.setItem(item);
                    allocation.setBatch(batch);
                    allocation.setQuantity(take);
                    allocation.setUnitCost(batch.getUnitCost() == null ? BigDecimal.ZERO : batch.getUnitCost());
                    allocation.setIsActive(true);
                    allocationRepository.save(allocation);
                    stock.record(invoice.getClinic(), item, actor, InventoryMovementAction.CONSUME, -take,
                            ClinicInventoryStockSupport.qty(batch),
                            "Invoice consumption (batch " + batch.getBatchNumber() + ")",
                            InventoryMovementSource.INVOICE, invoice.getUuid(), invoice.getInvoiceNumber(), batch);
                }
            }
            stock.syncAggregate(item, batches);
        }
    }

    @Transactional
    public void releaseExpiredReservations() {
        List<ClinicInventoryReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                InventoryReservationStatus.ACTIVE, LocalDateTime.now());
        if (expired == null) {
            return;
        }
        for (ClinicInventoryReservation reservation : expired) {
            releaseReservation(reservation);
        }
    }

    private void releaseReservation(ClinicInventoryReservation reservation) {
        reservation.setStatus(InventoryReservationStatus.RELEASED);
        reservationRepository.save(reservation);
        ClinicInventoryItem item = inventoryRepository
                .findByUuidAndClinic_IdAndIsActiveTrue(reservation.getItemUuid(), reservation.getClinic().getId())
                .orElse(null);
        int onHand = reservation.getBatch() != null ? ClinicInventoryStockSupport.qty(reservation.getBatch())
                : item == null ? 0 : onHand(item);
        ClinicInventoryMovement movement = new ClinicInventoryMovement();
        movement.setClinic(reservation.getClinic());
        movement.setItemUuid(reservation.getItemUuid());
        movement.setItemName(item == null ? reservation.getItemUuid() : item.getName());
        movement.setAction(InventoryMovementAction.RELEASE);
        movement.setQuantityDelta(reservation.getQuantity() == null ? 0 : reservation.getQuantity());
        movement.setQuantityAfter(onHand);
        movement.setNote("Reservation released");
        movement.setSource(InventoryMovementSource.INVOICE);
        movement.setInvoiceUuid(reservation.getInvoiceUuid());
        movement.setInvoiceNumber(reservation.getInvoiceNumber());
        movement.setBatch(reservation.getBatch());
        movement.setIsActive(true);
        movementRepository.save(movement);
    }
    static boolean isStocked(TreatmentInvoiceItemType itemType) {
        return itemType == TreatmentInvoiceItemType.MEDICINE
                || itemType == TreatmentInvoiceItemType.CONSUMABLE
                || itemType == TreatmentInvoiceItemType.VACCINATION
                || itemType == TreatmentInvoiceItemType.FOOD
                || itemType == TreatmentInvoiceItemType.EQUIPMENT;
    }

    static String expectedCategory(TreatmentInvoiceItemType itemType) {
        if (itemType == TreatmentInvoiceItemType.CONSUMABLE) {
            return KeyConstant.SUPPLY;
        }
        if (itemType == TreatmentInvoiceItemType.MEDICINE || itemType == TreatmentInvoiceItemType.VACCINATION) {
            return KeyConstant.MEDICATION;
        }
        if (itemType == TreatmentInvoiceItemType.FOOD) {
            return KeyConstant.FOOD;
        }
        if (itemType == TreatmentInvoiceItemType.EQUIPMENT) {
            return KeyConstant.EQUIPMENT;
        }
        return null;
    }

    private static void requireCategory(TreatmentInvoiceItemType itemType, ClinicInventoryItem item) {
        String expected = expectedCategory(itemType);
        String actual = item.getCategory() == null ? "" : item.getCategory().trim();
        if (expected == null || !expected.equalsIgnoreCase(actual)) {
            throw new CustomException(item.getName() + " is not in the " + expected + " catalog",
                    HttpStatus.BAD_REQUEST);
        }
    }

    static int requireWholeUnits(BigDecimal quantity, String itemName) {
        if (quantity == null) {
            throw new CustomException(itemName + " quantity is required", HttpStatus.BAD_REQUEST);
        }
        BigDecimal units = quantity.stripTrailingZeros();
        if (units.scale() > 0) {
            throw new CustomException(itemName + " must be billed in whole units", HttpStatus.BAD_REQUEST);
        }
        try {
            return units.intValueExact();
        } catch (ArithmeticException ex) {
            throw new CustomException(itemName + " quantity is too large", HttpStatus.BAD_REQUEST);
        }
    }

    private int reservationMinutes() {
        Integer minutes = environment.getProperty(AppConstant.INVENTORY_MOVEMENT_RELEASE_MINUTES, Integer.class, 5);
        return minutes.intValue();
    }

    private boolean alreadyConsumed(ConsultationInvoice invoice) {
        return movementRepository.existsByInvoiceUuidAndActionAndIsActiveTrue(invoice.getUuid(),
                InventoryMovementAction.CONSUME);
    }

    private List<TreatmentLineItemDto> stockedLines(ConsultationInvoice invoice) {
        return readItems(invoice.getLineItems()).stream()
                .filter(line -> isStocked(line.getItemType()) && blankToNull(line.getInventoryItemUuid()) != null)
                .toList();
    }

    private ClinicInventoryItem lockItem(ConsultationInvoice invoice, String itemUuid) {
        return inventoryRepository.findActiveForUpdate(itemUuid, invoice.getClinic().getId())
                .orElseThrow(() -> new ResourceNotFoundException("clinic inventory item", "uuid", itemUuid));
    }

    private static int onHand(ClinicInventoryItem item) {
        return item.getQuantity() == null ? 0 : item.getQuantity();
    }

    private User currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }

    private List<TreatmentLineItemDto> readItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<TreatmentLineItemDto>>() {
            });
        } catch (Exception e) {
            throw new CustomException("Invoice line items could not be read", HttpStatus.BAD_REQUEST);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
