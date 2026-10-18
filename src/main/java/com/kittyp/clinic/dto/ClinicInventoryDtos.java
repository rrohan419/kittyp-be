package com.kittyp.clinic.dto;

import java.math.BigDecimal;
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

    public record StockAdjustmentRequest(
            @NotNull StockDirection direction,
            @NotNull @Min(1) Integer quantity,
            @Size(max = 200) String note) {
    }

    public record InventoryItemRequest(
            @NotBlank String name,
            String category,
            @JsonAlias("stock") @NotNull @Min(0) Integer quantity,
            String unit,
            @NotNull @Min(0) Integer reorderLevel,
            @JsonAlias("price") @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
            Boolean active) {
    }

    public record InventoryItemModel(
            String uuid,
            String clinicUuid,
            String name,
            String category,
            Integer quantity,
            String unit,
            Integer reorderLevel,
            BigDecimal unitPrice,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Integer reserved) {
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
            LocalDateTime createdAt) {
    }
}