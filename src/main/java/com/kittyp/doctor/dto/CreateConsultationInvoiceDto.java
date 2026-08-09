package com.kittyp.doctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class CreateConsultationInvoiceDto {

    private String clinicUuid;
    private String petUuid;
    private String ownerUserUuid;

    /** Structured line items (preferred). */
    @Valid
    private List<TreatmentLineItemDto> items;

    /** Legacy raw JSON string when {@link #items} is empty. */
    private String lineItems;

    @DecimalMin(value = "0.00")
    private BigDecimal amount;

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal paidAmount;
    private BigDecimal balance;

    private String currency;
    private String notes;
    private String doctorNotes;
    private String nextVisitNotes;
    private String reason;
    private String diagnosis;
    private LocalDate consultationDate;

    private String paymentStatus;
    private String paymentMode;
    private String transactionId;

    private String petName;
    private String petSpecies;
    private String petBreed;
    private String petGender;
    private String petAge;
    private String petWeight;
    private String petMicrochip;
    private String patientId;

    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;
    private String ownerAddress;

    private String visitUuid;

    private Boolean generatePdf;

    /** When true with generatePdf, upload PDF to Meta and send invoice_receipt template. */
    private Boolean sendWhatsApp;
}
