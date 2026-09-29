package com.kittyp.doctor.dto;

import java.math.BigDecimal;

import com.kittyp.doctor.enums.TreatmentInvoiceItemType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TreatmentLineItemDto {

    @NotNull
    private TreatmentInvoiceItemType itemType;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantity;

    private String unit;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal unitPrice;

    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal total;
}
