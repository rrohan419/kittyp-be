package com.kittyp.doctor.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConsultationInvoiceDto {

    private String clinicUuid;
    private String petUuid;
    private String ownerUserUuid;

    @NotBlank
    private String lineItems;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal amount;

    private String currency;
    private String notes;
    private String pdfUrl;
}
