package com.kittyp.clinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ClinicInventoryDtos {

    private ClinicInventoryDtos() {
    }

    public enum StockDirection {
        ADD,
        REMOVE
    }

    public record InventoryEditAccess(boolean canEditInventory) {
    }

    /**
     * ADD receives a new batch (batchNumber/expiryDate/nonExpiring rules as in {@link BatchReceiptRequest}).
     * REMOVE takes from {@code batchUuid} when given (any batch, including expired/unreviewed, for write-off);
     * otherwise FEFO over eligible (non-expired, reviewed) batches only.
     */
    public record StockAdjustmentRequest(
            @NotNull StockDirection direction,
            @NotNull @Min(1) Integer quantity,
            @Size(max = 200) String note,
            String batchUuid,
            @Size(max = 100) String batchNumber,
            LocalDate expiryDate,
            Boolean nonExpiring,
            @DecimalMin("0.00") BigDecimal unitCost) {

        public StockAdjustmentRequest(StockDirection direction, Integer quantity, String note) {
            this(direction, quantity, note, null, null, null, null, null);
        }
    }

    /** Opening quantity > 0 needs expiryDate or nonExpiring=true (MEDICATION: expiryDate required). */
    public record InventoryItemRequest(
            @NotBlank String name,
            String category,
            @JsonAlias("stock") @NotNull @Min(0) Integer quantity,
            String unit,
            @NotNull @Min(0) Integer reorderLevel,
            @JsonAlias("price") @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            Boolean active,
            @Size(max = 100) String batchNumber,
            LocalDate expiryDate,
            Boolean nonExpiring,
            @DecimalMin("0.00") BigDecimal unitCost) {

        public InventoryItemRequest(String name, String category, Integer quantity, String unit, Integer reorderLevel,
                BigDecimal unitPrice, Boolean active) {
            this(name, category, quantity, unit, reorderLevel, unitPrice, active, null, null, null, null);
        }
    }

    public record BatchReceiptRequest(
            @Size(max = 100) String batchNumber,
            LocalDate expiryDate,
            Boolean nonExpiring,
            @NotNull @Min(1) Integer quantity,
            @DecimalMin("0.00") BigDecimal unitCost,
            @Size(max = 200) String note) {
    }

    /** Assigns batch/expiry data to a batch (typically legacy stock). Quantity is preserved. */
    public record BatchReviewRequest(
            @Size(max = 100) String batchNumber,
            LocalDate expiryDate,
            Boolean nonExpiring,
            @DecimalMin("0.00") BigDecimal unitCost,
            @Size(max = 200) String note) {
    }

    public record InventoryItemModel(
            String uuid,
            String clinicUuid,
            String name,
            String category,
            /** Physical aggregate on hand: every batch incl. expired and unreviewed. */
            Integer quantity,
            String unit,
            Integer reorderLevel,
            BigDecimal unitPrice,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            /** Active invoice holds (all batches). */
            Integer reserved,
            /** Billable now: eligible (reviewed, unexpired) quantity minus holds. */
            Integer availableQuantity,
            /** Reviewed, unexpired quantity before holds. */
            Integer eligibleQuantity,
            Integer expiredQuantity,
            /** Legacy / unbatched stock awaiting expiry review. Visible, never billable. */
            Integer unreviewedQuantity,
            /** Eligible quantity expiring within expiryWarningDays. */
            Integer expiringSoonQuantity,
            /** Earliest expiry among eligible dated batches, or null. */
            LocalDate nextExpiryDate,
            Boolean expiringSoon,
            Integer expiryWarningDays) {
    }

    public record InventoryBatchModel(
            String uuid,
            String itemUuid,
            String batchNumber,
            LocalDate expiryDate,
            Boolean nonExpiring,
            Integer quantityReceived,
            Integer quantityOnHand,
            Integer reserved,
            /** Billable from this batch now (0 unless status is ELIGIBLE/EXPIRING_SOON). */
            Integer availableQuantity,
            BigDecimal unitCost,
            LocalDateTime receivedAt,
            /** ELIGIBLE | EXPIRING_SOON | EXPIRED | UNREVIEWED */
            String status,
            Boolean reviewRequired,
            Long daysToExpiry) {
    }

    public record InventoryMovementModel(
            String uuid,
            String itemUuid,
            String itemName,
            String action,
            Integer quantityDelta,
            Integer quantityAfter,
            String note,
            String source,
            String invoiceUuid,
            String invoiceNumber,
            String actorName,
            LocalDateTime createdAt,
            String batchUuid,
            String batchNumber) {
    }
}
