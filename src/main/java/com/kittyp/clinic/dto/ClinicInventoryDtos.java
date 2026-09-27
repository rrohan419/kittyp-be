package com.kittyp.clinic.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.kittyp.clinic.enums.InventoryExpiryStatus;
import com.kittyp.clinic.enums.InventoryMovementType;
import com.kittyp.clinic.enums.InventoryStockStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ClinicInventoryDtos {

    private ClinicInventoryDtos() {
    }

    public static final Set<String> CATEGORIES = Set.of("medication", "supply", "equipment", "food");

    public record InventoryLotModel(
            String uuid,
            String itemUuid,
            String itemName,
            String lotNumber,
            String manufacturer,
            LocalDate manufacturedOn,
            LocalDate expiresOn,
            BigDecimal quantity,
            InventoryExpiryStatus expiryStatus) {
    }

    public record InventoryItemModel(
            String uuid,
            String name,
            String category,
            BigDecimal stock,
            String unit,
            Integer minStock,
            String barcode,
            String gtin,
            String sku,
            String manufacturer,
            Boolean trackStock,
            BigDecimal price,
            BigDecimal purchasePrice,
            InventoryStockStatus stockStatus,
            InventoryExpiryStatus expiryStatus,
            LocalDate earliestExpiry,
            List<InventoryLotModel> lots) {
    }

    /** Backward-compatible create/update: stock may seed initial lot quantity.
     * unit/barcode optional — create always stores unit as pcs; barcode only when provided (e.g. scan). */
    public record InventoryItemRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 32) String category,
            @NotNull @DecimalMin("0.0") BigDecimal stock,
            @NotNull @DecimalMin("0.0") BigDecimal price,
            @Size(max = 40) String unit,
            @Min(0) Integer minStock,
            @Size(max = 64) String barcode,
            @Size(max = 64) String gtin,
            @Size(max = 64) String sku,
            @Size(max = 200) String manufacturer,
            Boolean trackStock,
            @DecimalMin("0.0") BigDecimal purchasePrice,
            @Size(max = 80) String lotNumber,
            LocalDate manufacturedOn,
            LocalDate expiresOn) {
    }

    public record MovementRequest(
            @NotBlank String itemUuid,
            String lotUuid,
            @NotNull InventoryMovementType type,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @Size(max = 500) String notes,
            @Size(max = 80) String lotNumber,
            LocalDate manufacturedOn,
            LocalDate expiresOn,
            String manufacturer) {
    }

    public record MovementModel(
            String uuid,
            String itemUuid,
            String itemName,
            String lotUuid,
            String lotNumber,
            InventoryMovementType type,
            BigDecimal quantity,
            BigDecimal previousQty,
            BigDecimal newQty,
            String invoiceUuid,
            String notes,
            LocalDateTime createdAt) {
    }

    public record ScanRequest(@NotBlank @Size(max = 256) String rawCode) {
    }

    public record ScanResult(
            boolean found,
            String rawCode,
            String gtin,
            String lotNumber,
            LocalDate expiresOn,
            String serial,
            InventoryItemModel item,
            String suggestedName) {
    }

    public record ConsumptionRow(String itemUuid, String name, BigDecimal quantity) {
    }

    public record DashboardModel(
            long totalItems,
            long lowStockCount,
            long outOfStockCount,
            long expiringSoonCount,
            long expiredCount,
            BigDecimal totalStockQuantity,
            BigDecimal stockValue,
            BigDecimal consumed7d,
            BigDecimal consumed30d,
            BigDecimal consumed90d,
            List<InventoryItemModel> lowStock,
            List<InventoryLotModel> expiringSoon,
            List<InventoryLotModel> expired,
            List<ConsumptionRow> mostConsumed30d,
            List<ConsumptionRow> leastConsumed30d,
            List<InventoryItemModel> recentlyAdded,
            List<InventoryItemModel> recentlyUpdated,
            List<MovementModel> recentMovements) {
    }

    public record WeeklyReportModel(
            String clinicUuid,
            String clinicName,
            LocalDate weekStart,
            LocalDate weekEnd,
            List<InventoryItemModel> lowStock,
            List<InventoryItemModel> outOfStock,
            List<InventoryLotModel> expiringSoon,
            List<InventoryLotModel> expired,
            List<ConsumptionRow> mostConsumed,
            List<ConsumptionRow> leastConsumed,
            BigDecimal stockAdded,
            BigDecimal stockConsumed,
            List<MovementModel> significantChanges) {
    }

    public record AlertModel(
            String alertType,
            String itemUuid,
            String itemName,
            String message,
            LocalDateTime lastNotifiedAt,
            boolean resolved) {
    }
}
