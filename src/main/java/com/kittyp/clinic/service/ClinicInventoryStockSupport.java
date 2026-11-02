package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryBatch;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryMovementSource;
import com.kittyp.clinic.enums.InventoryReservationStatus;
import com.kittyp.clinic.repository.ClinicInventoryBatchRepository;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.clinic.repository.ClinicInventoryReservationRepository;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.AlphanumericIdService;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

/**
 * Batch/expiry rules shared by inventory management and invoice consumption.
 * Every method that changes quantities expects the caller to hold the item row lock
 * ({@code ClinicInventoryItemRepository#findActiveForUpdate}).
 *
 * Expiry semantics: expiryDate is the last usable day; a batch is EXPIRED when expiryDate &lt; today.
 */
@Component
@RequiredArgsConstructor
public class ClinicInventoryStockSupport {

    public static final int EXPIRY_WARNING_DAYS = 90;
    public static final String LEGACY_BATCH_NUMBER = "LEGACY";

    public enum BatchStatus {
        ELIGIBLE, EXPIRING_SOON, EXPIRED, UNREVIEWED
    }

    /** Billable capacity of one batch after holds, in FEFO order. */
    public record Slot(ClinicInventoryBatch batch, int available) {
    }

    public record ItemStock(int physical, int eligible, int available, int expired, int unreviewed,
            int expiringSoon, LocalDate nextExpiry) {
    }

    private final ClinicInventoryBatchRepository batchRepository;
    private final ClinicInventoryItemRepository itemRepository;
    private final ClinicInventoryReservationRepository reservationRepository;
    private final ClinicInventoryMovementRepository movementRepository;

    // ---- pure rules ----

    public static BatchStatus status(ClinicInventoryBatch batch, LocalDate today) {
        if (Boolean.TRUE.equals(batch.getReviewRequired())) {
            return BatchStatus.UNREVIEWED;
        }
        if (Boolean.TRUE.equals(batch.getNonExpiring())) {
            return BatchStatus.ELIGIBLE;
        }
        LocalDate expiry = batch.getExpiryDate();
        if (expiry == null) {
            return BatchStatus.UNREVIEWED; // unknown expiry is never silently non-expiring
        }
        if (expiry.isBefore(today)) {
            return BatchStatus.EXPIRED;
        }
        return expiry.isAfter(today.plusDays(EXPIRY_WARNING_DAYS)) ? BatchStatus.ELIGIBLE : BatchStatus.EXPIRING_SOON;
    }

    public static boolean eligible(ClinicInventoryBatch batch, LocalDate today) {
        BatchStatus s = status(batch, today);
        return s == BatchStatus.ELIGIBLE || s == BatchStatus.EXPIRING_SOON;
    }

    public static int qty(ClinicInventoryBatch batch) {
        return batch.getQuantityOnHand() == null ? 0 : batch.getQuantityOnHand();
    }

    /** First-expired-first-out; non-expiring last; ties by receipt then id. */
    public static final Comparator<ClinicInventoryBatch> FEFO = Comparator
            .comparing((ClinicInventoryBatch b) -> Boolean.TRUE.equals(b.getNonExpiring()) || b.getExpiryDate() == null
                    ? LocalDate.MAX
                    : b.getExpiryDate())
            .thenComparing(b -> b.getReceivedAt() == null ? LocalDateTime.MIN : b.getReceivedAt())
            .thenComparing(b -> b.getId() == null ? 0L : b.getId());

    public static boolean isMedication(String category) {
        return category != null && KeyConstant.MEDICATION.equalsIgnoreCase(category.trim());
    }

    /**
     * Validates expiry inputs. MEDICATION (incl. vaccines) needs an expiry date; others need a date or an
     * explicit nonExpiring=true. Received stock may not already be expired; review may assign past dates.
     */
    public static void validateExpiry(String category, LocalDate expiryDate, Boolean nonExpiring,
            boolean allowPastDate, LocalDate today) {
        boolean nonExp = Boolean.TRUE.equals(nonExpiring);
        if (nonExp && expiryDate != null) {
            throw new CustomException("Provide either an expiry date or mark the stock non-expiring, not both",
                    HttpStatus.BAD_REQUEST);
        }
        if (isMedication(category)) {
            if (nonExp) {
                throw new CustomException("Medication and vaccine stock cannot be non-expiring; enter an expiry date",
                        HttpStatus.BAD_REQUEST);
            }
            if (expiryDate == null) {
                throw new CustomException("Expiry date is required for medication and vaccine stock",
                        HttpStatus.BAD_REQUEST);
            }
        } else if (!nonExp && expiryDate == null) {
            throw new CustomException("Enter an expiry date or explicitly mark the stock as non-expiring",
                    HttpStatus.BAD_REQUEST);
        }
        if (!allowPastDate && expiryDate != null && expiryDate.isBefore(today)) {
            throw new CustomException("Expired stock cannot be received (expiry " + expiryDate + ")",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /** FEFO slots of eligible batches after active holds. {@code batches} may be unordered. */
    public static List<Slot> slots(List<ClinicInventoryBatch> batches, LocalDate today, Map<Long, Integer> heldByBatch,
            int batchlessHeld) {
        List<ClinicInventoryBatch> ordered = new ArrayList<>(batches);
        ordered.sort(FEFO);
        List<Slot> out = new ArrayList<>();
        int skip = Math.max(0, batchlessHeld);
        for (ClinicInventoryBatch b : ordered) {
            if (!eligible(b, today) || qty(b) <= 0) {
                continue;
            }
            int free = qty(b) - heldByBatch.getOrDefault(b.getId(), 0);
            if (free <= 0) {
                continue;
            }
            int cut = Math.min(skip, free);
            skip -= cut;
            free -= cut;
            if (free > 0) {
                out.add(new Slot(b, free));
            }
        }
        return out;
    }

    public static ItemStock summarize(ClinicInventoryItem item, List<ClinicInventoryBatch> batches, LocalDate today,
            Map<Long, Integer> heldByBatch, int batchlessHeld) {
        int physical = item.getQuantity() == null ? 0 : item.getQuantity();
        int batchTotal = 0;
        int eligible = 0;
        int expired = 0;
        int unreviewed = 0;
        int soon = 0;
        LocalDate next = null;
        for (ClinicInventoryBatch b : batches) {
            int q = qty(b);
            batchTotal += q;
            if (q <= 0) {
                continue;
            }
            switch (status(b, today)) {
                case EXPIRED -> expired += q;
                case UNREVIEWED -> unreviewed += q;
                case EXPIRING_SOON -> {
                    eligible += q;
                    soon += q;
                }
                default -> eligible += q;
            }
            if (eligible(b, today) && !Boolean.TRUE.equals(b.getNonExpiring()) && b.getExpiryDate() != null
                    && (next == null || b.getExpiryDate().isBefore(next))) {
                next = b.getExpiryDate();
            }
        }
        unreviewed += Math.max(0, physical - batchTotal); // not yet migrated: still legacy
        int available = slots(batches, today, heldByBatch, batchlessHeld).stream().mapToInt(Slot::available).sum();
        return new ItemStock(physical, eligible, available, expired, unreviewed, soon, next);
    }

    // ---- persistence helpers (caller holds item lock) ----

    public List<ClinicInventoryBatch> batches(ClinicInventoryItem item) {
        return new ArrayList<>(batchRepository.findByItem_IdAndIsActiveTrue(item.getId()));
    }

    public Map<Long, Integer> heldByBatch(ClinicInventoryItem item) {
        Map<Long, Integer> held = new HashMap<>();
        List<Object[]> rows = reservationRepository.sumActiveByBatchForItem(item.getUuid(),
                InventoryReservationStatus.ACTIVE, LocalDateTime.now());
        if (rows != null) {
            for (Object[] row : rows) {
                held.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
            }
        }
        return held;
    }

    public List<Slot> slots(ClinicInventoryItem item, List<ClinicInventoryBatch> batches, LocalDate today) {
        return slots(batches, today, heldByBatch(item), reservationRepository.sumActiveWithoutBatch(item.getUuid(),
                InventoryReservationStatus.ACTIVE, LocalDateTime.now()));
    }

    /** Moves any quantity not backed by batches into the review-required legacy batch. */
    public void ensureLegacyBatch(ClinicInventoryItem item, List<ClinicInventoryBatch> batches) {
        int physical = item.getQuantity() == null ? 0 : item.getQuantity();
        int total = batches.stream().mapToInt(ClinicInventoryStockSupport::qty).sum();
        int gap = physical - total;
        if (gap <= 0) {
            return;
        }
        ClinicInventoryBatch legacy = batches.stream()
                .filter(b -> Boolean.TRUE.equals(b.getReviewRequired()) && LEGACY_BATCH_NUMBER.equals(b.getBatchNumber()))
                .findFirst().orElse(null);
        if (legacy == null) {
            legacy = ClinicInventoryBatch.builder().clinic(item.getClinic()).item(item)
                    .batchNumber(LEGACY_BATCH_NUMBER).nonExpiring(false).reviewRequired(true).quantityReceived(0)
                    .quantityOnHand(0).unitCost(BigDecimal.ZERO).receivedAt(LocalDateTime.now()).build();
            legacy.setIsActive(true);
            batches.add(legacy);
        }
        legacy.setQuantityReceived(legacy.getQuantityReceived() + gap);
        legacy.setQuantityOnHand(qty(legacy) + gap);
        batchRepository.save(legacy);
    }

    /** Re-derives the item's aggregate quantity from its batches. */
    public int syncAggregate(ClinicInventoryItem item, List<ClinicInventoryBatch> batches) {
        int total = batches.stream().mapToInt(ClinicInventoryStockSupport::qty).sum();
        item.setQuantity(total);
        itemRepository.save(item);
        return total;
    }

    public ClinicInventoryBatch receive(ClinicInventoryItem item, List<ClinicInventoryBatch> batches, String batchNumber,
            LocalDate expiry, Boolean nonExpiring, int quantity, BigDecimal unitCost, LocalDate today) {
        validateExpiry(item.getCategory(), expiry, nonExpiring, false, today);
        boolean nonExp = Boolean.TRUE.equals(nonExpiring);
        String number = batchNumber == null || batchNumber.isBlank() ? null : batchNumber.trim();
        if (number != null && number.equalsIgnoreCase(LEGACY_BATCH_NUMBER)) {
            throw new CustomException("Batch number " + LEGACY_BATCH_NUMBER + " is reserved", HttpStatus.BAD_REQUEST);
        }
        BigDecimal cost = unitCost == null ? BigDecimal.ZERO : unitCost;
        ClinicInventoryBatch batch = null;
        if (number != null) {
            for (ClinicInventoryBatch b : batches) {
                if (number.equalsIgnoreCase(b.getBatchNumber())) {
                    batch = b;
                    break;
                }
            }
        }
        if (batch != null) {
            boolean same = Boolean.TRUE.equals(batch.getNonExpiring()) == nonExp
                    && java.util.Objects.equals(batch.getExpiryDate(), expiry)
                    && !Boolean.TRUE.equals(batch.getReviewRequired());
            if (!same) {
                throw new CustomException("Batch " + number + " already exists with a different expiry", HttpStatus.CONFLICT);
            }
            batch.setQuantityReceived(batch.getQuantityReceived() + quantity);
            batch.setQuantityOnHand(qty(batch) + quantity);
        } else {
            batch = ClinicInventoryBatch.builder().clinic(item.getClinic()).item(item)
                    .batchNumber(number != null ? number : "B-" + today.toString().replace("-", "") + "-"
                            + AlphanumericIdService.generate())
                    .expiryDate(nonExp ? null : expiry).nonExpiring(nonExp).reviewRequired(false)
                    .quantityReceived(quantity).quantityOnHand(quantity).unitCost(cost)
                    .receivedAt(LocalDateTime.now()).build();
            batch.setIsActive(true);
            batches.add(batch);
        }
        return batchRepository.save(batch);
    }

    public void saveBatch(ClinicInventoryBatch batch) {
        batchRepository.save(batch);
    }

    public ClinicInventoryMovement record(Clinic clinic, ClinicInventoryItem item, User actor,
            InventoryMovementAction action, int delta, int after, String note, InventoryMovementSource source,
            String invoiceUuid, String invoiceNumber, ClinicInventoryBatch batch) {
        ClinicInventoryMovement movement = new ClinicInventoryMovement();
        movement.setClinic(clinic);
        movement.setItemUuid(item.getUuid());
        movement.setItemName(item.getName());
        movement.setActor(actor);
        movement.setActorName(ClinicInventoryServiceImpl.displayName(actor));
        movement.setAction(action);
        movement.setQuantityDelta(delta);
        movement.setQuantityAfter(after);
        movement.setNote(note == null || note.length() <= 500 ? note : note.substring(0, 500));
        movement.setSource(source);
        movement.setInvoiceUuid(invoiceUuid);
        movement.setInvoiceNumber(invoiceNumber);
        movement.setBatch(batch);
        movement.setIsActive(true);
        return movementRepository.save(movement);
    }

    public static long daysToExpiry(ClinicInventoryBatch batch, LocalDate today) {
        return batch.getExpiryDate() == null || Boolean.TRUE.equals(batch.getNonExpiring()) ? Long.MAX_VALUE
                : ChronoUnit.DAYS.between(today, batch.getExpiryDate());
    }
}
