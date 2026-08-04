package com.kittyp.doctor.dto;

import com.kittyp.doctor.enums.ConsultationInvoiceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateConsultationInvoiceStatusDto {

    @NotNull
    private ConsultationInvoiceStatus status;
}
