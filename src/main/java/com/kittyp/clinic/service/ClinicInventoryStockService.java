package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryLot;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.InventoryMovementType;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryLotRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicInventoryStockService {

    private final ClinicInventoryItemRepository itemRepository;
    private final ClinicInventoryLotRepository lotRepository;
    private final ClinicInventoryMovementRepository movementRepository;

    @Transactional
    public ClinicInventoryLot ensureDefaultLot(ClinicInventoryItem item) {
        List<ClinicInventoryLot> lots =
                lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId());
        if (!lots.isEmpty()) {
            return lots.get(0);
        }
        BigDecimal qty = nz(item.getStock());
        ClinicInventoryLot lot = ClinicInventoryLot.builder()
                .clinic(item.getClinic())
                .inventoryItem(item)
                .lotNumber("DEFAULT")
                .quantity(qty)
                .build();
        lot.setIsActive(true);
        return lotRepository.save(lot);
    }

    @Transactional
    public void syncItemStockFromLots(ClinicInventoryItem item) {
        List<ClinicInventoryLot> lots =
                lotRepository.findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId());
        BigDecimal sum = lots.stream().map(l -> nz(l.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        item.setStock(sum.setScale(3, RoundingMode.HALF_UP));
        itemRepository.save(item);
    }

    /**
     * Manual stock movement (IN creates/adds lot; OUT/EXPIRED/DAMAGED/RETURN/ADJUST apply to lot).
     */
    @Transactional
    public ClinicInventoryMovement applyManual(
            Clinic clinic,
            ClinicInventoryItem item,
            String lotUuid,
            InventoryMovementType type,
            BigDecimal quantity,
            String notes,
            String lotNumber,
            LocalDate manufacturedOn,
            LocalDate expiresOn,
            String manufacturer,
            User actor) {
        if (!Boolean.TRUE.equals(item.getTrackStock())) {
            throw new CustomException("Item is not stock-tracked", HttpStatus.BAD_REQUEST);
        }
        BigDecimal qty = requirePositive(quantity);
        ClinicInventoryItem locked = itemRepository
                .findByUuidForUpdate(item.getUuid(), clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "uuid", item.getUuid()));

        return switch (type) {
            case STOCK_IN, RETURN -> stockIn(
                    clinic, locked, lotUuid, type, qty, notes, lotNumber, manufacturedOn, expiresOn, manufacturer,
                    actor);
            case STOCK_OUT, EXPIRED, DAMAGED -> stockOut(clinic, locked, lotUuid, type, qty, notes, actor, null, null);
            case ADJUSTMENT -> adjust(clinic, locked, lotUuid, qty, notes, lotNumber, manufacturedOn, expiresOn,
                    manufacturer, actor);
        };
    }

    /**
     * Idempotent invoice consumption with FEFO when lotUuid is null.
     */
    @Transactional
    public void deductForInvoiceLine(
            Clinic clinic,
            String itemUuid,
            String lotUuid,
            BigDecimal quantity,
            String invoiceUuid,
            String lineKey,
            User actor) {
        if (clinic == null || !StringUtils.hasText(itemUuid) || !StringUtils.hasText(invoiceUuid)) {
            return;
        }
        ClinicInventoryItem locked = itemRepository
                .findByUuidForUpdate(itemUuid, clinic.getId())
                .orElseThrow(() -> new CustomException(
                        "Inventory item not found for clinic: " + itemUuid, HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(locked.getTrackStock())) {
            return;
        }
        if (movementRepository.existsByInvoiceUuidAndInventoryItem_IdAndInvoiceLineKey(
                invoiceUuid, locked.getId(), lineKey)) {
            return;
        }
        BigDecimal qty = requirePositive(quantity);
        stockOut(clinic, locked, lotUuid, InventoryMovementType.STOCK_OUT, qty, "Invoice " + invoiceUuid, actor,
                invoiceUuid, lineKey);
    }

    private ClinicInventoryMovement stockIn(
            Clinic clinic,
            ClinicInventoryItem item,
            String lotUuid,
            InventoryMovementType type,
            BigDecimal qty,
            String notes,
            String lotNumber,
            LocalDate manufacturedOn,
            LocalDate expiresOn,
            String manufacturer,
            User actor) {
        BigDecimal previous = nz(item.getStock());
        ClinicInventoryLot lot;
        if (StringUtils.hasText(lotUuid)) {
            lot = lotRepository
                    .findByUuidForUpdate(lotUuid, clinic.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryLot", "uuid", lotUuid));
            if (!lot.getInventoryItem().getId().equals(item.getId())) {
                throw new CustomException("Lot does not belong to item", HttpStatus.BAD_REQUEST);
            }
            lot.setQuantity(nz(lot.getQuantity()).add(qty));
            lotRepository.save(lot);
        } else {
            lot = ClinicInventoryLot.builder()
                    .clinic(clinic)
                    .inventoryItem(item)
                    .lotNumber(StringUtils.hasText(lotNumber) ? lotNumber.trim() : null)
                    .manufacturer(StringUtils.hasText(manufacturer) ? manufacturer.trim() : item.getManufacturer())
                    .manufacturedOn(manufacturedOn)
                    .expiresOn(expiresOn)
                    .quantity(qty)
                    .build();
            lot.setIsActive(true);
            lot = lotRepository.save(lot);
        }
        BigDecimal next = previous.add(qty).setScale(3, RoundingMode.HALF_UP);
        item.setStock(next);
        itemRepository.save(item);
        return saveMovement(clinic, item, lot, type, qty, previous, next, notes, null, null, actor);
    }

    private ClinicInventoryMovement stockOut(
            Clinic clinic,
            ClinicInventoryItem item,
            String lotUuid,
            InventoryMovementType type,
            BigDecimal qty,
            String notes,
            User actor,
            String invoiceUuid,
            String lineKey) {
        BigDecimal previous = nz(item.getStock());
        if (previous.compareTo(qty) < 0) {
            throw new CustomException(
                    "Insufficient stock for " + item.getName() + ". Available: " + previous.stripTrailingZeros()
                            .toPlainString() + ", requested: " + qty.stripTrailingZeros().toPlainString(),
                    HttpStatus.BAD_REQUEST);
        }
        ensureDefaultLot(item);
        List<Allocation> allocations = allocateOut(clinic, item, lotUuid, qty);
        ClinicInventoryLot primaryLot = null;
        for (Allocation a : allocations) {
            ClinicInventoryLot lot = a.lot();
            lot.setQuantity(nz(lot.getQuantity()).subtract(a.qty()).setScale(3, RoundingMode.HALF_UP));
            lotRepository.save(lot);
            if (primaryLot == null) {
                primaryLot = lot;
            }
        }
        BigDecimal next = previous.subtract(qty).setScale(3, RoundingMode.HALF_UP);
        item.setStock(next);
        itemRepository.save(item);
        return saveMovement(clinic, item, primaryLot, type, qty, previous, next, notes, invoiceUuid, lineKey, actor);
    }

    private ClinicInventoryMovement adjust(
            Clinic clinic,
            ClinicInventoryItem item,
            String lotUuid,
            BigDecimal qty,
            String notes,
            String lotNumber,
            LocalDate manufacturedOn,
            LocalDate expiresOn,
            String manufacturer,
            User actor) {
        // ADJUSTMENT quantity is absolute target for the lot (or creates lot). Prefer delta via notes convention:
        // Here we treat quantity as the delta amount to set lot to (absolute) when lot provided; if no lot, set item.
        // Plan: ADJUSTMENT with positive qty = set lot quantity to this value.
        BigDecimal previous = nz(item.getStock());
        ClinicInventoryLot lot;
        if (StringUtils.hasText(lotUuid)) {
            lot = lotRepository
                    .findByUuidForUpdate(lotUuid, clinic.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryLot", "uuid", lotUuid));
            if (!lot.getInventoryItem().getId().equals(item.getId())) {
                throw new CustomException("Lot does not belong to item", HttpStatus.BAD_REQUEST);
            }
            lot.setQuantity(qty.setScale(3, RoundingMode.HALF_UP));
            if (manufacturedOn != null) {
                lot.setManufacturedOn(manufacturedOn);
            }
            if (expiresOn != null) {
                lot.setExpiresOn(expiresOn);
            }
            if (StringUtils.hasText(lotNumber)) {
                lot.setLotNumber(lotNumber.trim());
            }
            if (StringUtils.hasText(manufacturer)) {
                lot.setManufacturer(manufacturer.trim());
            }
            lotRepository.save(lot);
        } else {
            lot = ensureDefaultLot(item);
            lot = lotRepository
                    .findByUuidForUpdate(lot.getUuid(), clinic.getId())
                    .orElse(lot);
            lot.setQuantity(qty.setScale(3, RoundingMode.HALF_UP));
            lotRepository.save(lot);
        }
        syncItemStockFromLots(item);
        ClinicInventoryItem refreshed = itemRepository.findById(item.getId()).orElse(item);
        BigDecimal next = nz(refreshed.getStock());
        BigDecimal delta = next.subtract(previous).abs();
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            delta = BigDecimal.ZERO;
        }
        return saveMovement(
                clinic,
                refreshed,
                lot,
                InventoryMovementType.ADJUSTMENT,
                delta.max(BigDecimal.valueOf(0.001)),
                previous,
                next,
                notes,
                null,
                null,
                actor);
    }

    private List<Allocation> allocateOut(Clinic clinic, ClinicInventoryItem item, String lotUuid, BigDecimal qty) {
        List<Allocation> result = new ArrayList<>();
        if (StringUtils.hasText(lotUuid)) {
            ClinicInventoryLot lot = lotRepository
                    .findByUuidForUpdate(lotUuid, clinic.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("InventoryLot", "uuid", lotUuid));
            if (!lot.getInventoryItem().getId().equals(item.getId())) {
                throw new CustomException("Lot does not belong to item", HttpStatus.BAD_REQUEST);
            }
            if (nz(lot.getQuantity()).compareTo(qty) < 0) {
                throw new CustomException(
                        "Insufficient stock in selected lot for " + item.getName(), HttpStatus.BAD_REQUEST);
            }
            result.add(new Allocation(lot, qty));
            return result;
        }
        List<ClinicInventoryLot> fefo = lotRepository.findFefoLotsForUpdate(item.getId(), clinic.getId());
        BigDecimal remaining = qty;
        for (ClinicInventoryLot lot : fefo) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = nz(lot.getQuantity());
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal take = available.min(remaining);
            result.add(new Allocation(lot, take));
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new CustomException("Insufficient stock for " + item.getName(), HttpStatus.BAD_REQUEST);
        }
        return result;
    }

    private ClinicInventoryMovement saveMovement(
            Clinic clinic,
            ClinicInventoryItem item,
            ClinicInventoryLot lot,
            InventoryMovementType type,
            BigDecimal qty,
            BigDecimal previous,
            BigDecimal next,
            String notes,
            String invoiceUuid,
            String lineKey,
            User actor) {
        ClinicInventoryMovement mov = ClinicInventoryMovement.builder()
                .clinic(clinic)
                .inventoryItem(item)
                .lot(lot)
                .type(type)
                .quantity(qty.setScale(3, RoundingMode.HALF_UP))
                .previousQty(previous.setScale(3, RoundingMode.HALF_UP))
                .newQty(next.setScale(3, RoundingMode.HALF_UP))
                .invoiceUuid(invoiceUuid)
                .invoiceLineKey(lineKey)
                .notes(notes)
                .actor(actor)
                .build();
        mov.setIsActive(true);
        return movementRepository.save(mov);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal requirePositive(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Quantity must be greater than zero", HttpStatus.BAD_REQUEST);
        }
        return quantity.setScale(3, RoundingMode.HALF_UP);
    }

    private record Allocation(ClinicInventoryLot lot, BigDecimal qty) {
    }
}
