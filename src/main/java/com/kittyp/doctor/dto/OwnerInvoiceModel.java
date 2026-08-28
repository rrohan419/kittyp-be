package com.kittyp.doctor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pet-owner view of a consultation invoice — no Razorpay ids or clinic internals.
 */
public record OwnerInvoiceModel(
        String uuid,
        String invoiceNumber,
        String visitUuid,
        String petUuid,
        String clinicName,
        String doctorName,
        String status,
        String paymentStatus,
        BigDecimal amount,
        BigDecimal paidAmount,
        String currency,
        String diagnosis,
        String reason,
        String doctorNotes,
        String nextVisitNotes,
        boolean pdfAvailable,
        LocalDate consultationDate,
        LocalDateTime createdAt) {
}
