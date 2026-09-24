package com.kittyp.clinic.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ClinicInventoryDtos {

    private ClinicInventoryDtos() {
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
            LocalDateTime updatedAt) {
    }
}