package com.kittyp.doctor.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thymeleaf view-model for veterinary tax / medical treatment invoices.
 * Separate from retail product {@code InvoiceData}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentInvoiceData {

    private String title;
    private String invoiceNumber;
    private String invoiceDate;
    private String paymentStatus;
    private String paymentStatusLabel;
    private String paymentSummary;
    private String paymentMode;
    private String paymentModeLabel;
    private String transactionId;
    private String razorpayOrderId;
    private String paidAt;
    private boolean paid;
    private boolean partial;

    private String clinicName;
    private String clinicAddress;
    private String clinicPhone;
    private String clinicEmail;
    private String clinicGstin;
    private String clinicRegistrationNumber;

    private String doctorName;

    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;
    private String ownerAddress;

    private String petName;
    private String petSpecies;
    private String petBreed;
    private String petGender;
    private String petAge;
    private String petWeight;
    private String petMicrochip;
    private String patientId;

    private String consultationDate;
    private String reason;
    private String diagnosis;

    @Builder.Default
    private List<LineItem> services = new ArrayList<>();
    @Builder.Default
    private List<LineItem> medicines = new ArrayList<>();
    @Builder.Default
    private List<LineItem> consumables = new ArrayList<>();
    @Builder.Default
    private List<LineItem> laboratory = new ArrayList<>();
    @Builder.Default
    private List<LineItem> surgery = new ArrayList<>();
    @Builder.Default
    private List<LineItem> hospitalization = new ArrayList<>();
    @Builder.Default
    private List<LineItem> other = new ArrayList<>();

    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal taxTotal;
    private BigDecimal grandTotal;
    private BigDecimal paidAmount;
    private BigDecimal balance;

    private String doctorNotes;
    private String nextVisitNotes;
    private String currency;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal rate;
        private BigDecimal amount;
        private String itemType;
    }
}
