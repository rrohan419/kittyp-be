package com.kittyp.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarkInvoicePaidDto {

    @NotBlank
    @Size(max = 40)
    private String paymentMode;

    @Size(max = 120)
    private String transactionId;
}
