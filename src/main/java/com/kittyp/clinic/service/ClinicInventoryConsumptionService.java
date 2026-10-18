package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.kittyp.clinic.entity.ClinicInventoryItem;
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

    /** Hold stock for a draft. Does not reduce on-hand quantity. Short lines are left unreserved. */
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
        for (TreatmentLineItemDto line : stockedLines(invoice)) {
            ClinicInventoryItem item = lockItem(invoice, line.getInventoryItemUuid());
            requireCategory(line.getItemType(), item);
            int units = requireWholeUnits(line.getQuantity(), item.getName());
            int onHand = onHand(item);
            int available = onHand - reservedNow(item.getUuid());
            if (units > available) {
                continue;
            }
            ClinicInventoryReservation reservation = ClinicInventoryReservation.builder()
                    .clinic(invoice.getClinic())
                    .itemUuid(item.getUuid())
                    .invoiceUuid(invoice.getUuid())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .quantity(units)
                    .expiresAt(expiresAt)
                    .status(InventoryReservationStatus.ACTIVE)
                    .build();
            reservation.setIsActive(true);
            reservationRepository.save(reservation);
            record(invoice, item, actor, InventoryMovementAction.RESERVE, -units, onHand,
                    "Reserved " + units + " for " + reservationMinutes + " minutes");
        }
    }

    /** PDF generation and mark paid. An active hold is consumed without asking again. */
    @Transactional
    public void consumeForIssue(ConsultationInvoice invoice) {
        if (invoice.getClinic() == null || invoice.getUuid() == null || alreadyConsumed(invoice)) {
            return;
        }
        releaseExpiredReservations();
        User actor = currentActor();
        LocalDateTime now = LocalDateTime.now();
        for (TreatmentLineItemDto line : stockedLines(invoice)) {
            ClinicInventoryItem item = lockItem(invoice, line.getInventoryItemUuid());
            requireCategory(line.getItemType(), item);
            int units = requireWholeUnits(line.getQuantity(), item.getName());
            int onHand = onHand(item);
            Optional<ClinicInventoryReservation> found = reservationRepository
                    .findFirstByInvoiceUuidAndItemUuidAndStatusAndExpiresAtAfter(invoice.getUuid(), item.getUuid(),
                            InventoryReservationStatus.ACTIVE, now);
            ClinicInventoryReservation hold = found == null ? null
                    : found.filter(reservation -> reservation.getQuantity() != null && reservation.getQuantity() >= units)
                            .orElse(null);
            int held = hold == null || hold.getQuantity() == null ? 0 : hold.getQuantity();
            int available = onHand - Math.max(0, reservedNow(item.getUuid()) - held);
            int free = Math.max(available, 0);
            if (units > free) {
                String unit = item.getUnit() == null || item.getUnit().isBlank() ? "units" : item.getUnit();
                throw new CustomException(
                        "Only " + free + " " + item.getName() + " on hand. Add " + (units - free) + " " + unit
                                + " in inventory before billing " + units + ".",
                        HttpStatus.CONFLICT);
            }
            int after = onHand - units;
            item.setQuantity(after);
            inventoryRepository.save(item);
            if (hold != null) {
                hold.setStatus(InventoryReservationStatus.CONSUMED);
                reservationRepository.save(hold);
            }
            record(invoice, item, actor, InventoryMovementAction.CONSUME, -units, after, "Invoice consumption");
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
        int onHand = item == null ? 0 : onHand(item);
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

    private int reservedNow(String itemUuid) {
        return reservationRepository.sumActiveQuantity(itemUuid, InventoryReservationStatus.ACTIVE,
                LocalDateTime.now());
    }

    private static int onHand(ClinicInventoryItem item) {
        return item.getQuantity() == null ? 0 : item.getQuantity();
    }

    private void record(ConsultationInvoice invoice, ClinicInventoryItem item, User actor, InventoryMovementAction action,
            int delta, int after, String note) {
        ClinicInventoryMovement movement = new ClinicInventoryMovement();
        movement.setClinic(invoice.getClinic());
        movement.setItemUuid(item.getUuid());
        movement.setItemName(item.getName());
        movement.setActor(actor);
        movement.setActorName(ClinicInventoryServiceImpl.displayName(actor));
        movement.setAction(action);
        movement.setQuantityDelta(delta);
        movement.setQuantityAfter(after);
        movement.setNote(note);
        movement.setSource(InventoryMovementSource.INVOICE);
        movement.setInvoiceUuid(invoice.getUuid());
        movement.setInvoiceNumber(invoice.getInvoiceNumber());
        movement.setIsActive(true);
        movementRepository.save(movement);
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
