package com.kittyp.doctor.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.doctor.enums.ConsultationInvoiceStatus;
import com.kittyp.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "consultation_invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ConsultationInvoice extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String uuid;

    @Column(name = "invoice_number", unique = true, length = 40)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @Column(name = "pet_uuid")
    private String petUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    /** JSON array of line items with itemType, description, quantity, unitPrice, total. */
    @Column(name = "line_items", columnDefinition = "TEXT", nullable = false)
    private String lineItems;

    /** JSON snapshot: petName, species, breed, gender, age, weight, microchip, patientId */
    @Column(name = "pet_snapshot", columnDefinition = "TEXT")
    private String petSnapshot;

    /** JSON snapshot: ownerName, phone, email, address */
    @Column(name = "owner_snapshot", columnDefinition = "TEXT")
    private String ownerSnapshot;

    @Column(name = "consultation_date")
    private LocalDate consultationDate;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(precision = 19, scale = 2)
    private BigDecimal tax;

    @Column(precision = 19, scale = 2)
    private BigDecimal cgst;

    @Column(precision = 19, scale = 2)
    private BigDecimal sgst;

    @Column(precision = 19, scale = 2)
    private BigDecimal igst;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "payment_status", length = 30)
    private String paymentStatus;

    @Column(name = "payment_mode", length = 40)
    private String paymentMode;

    @Column(name = "transaction_id", length = 120)
    private String transactionId;

    @Column(name = "visit_uuid", length = 64)
    private String visitUuid;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConsultationInvoiceStatus status = ConsultationInvoiceStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    @Column(name = "next_visit_notes", columnDefinition = "TEXT")
    private String nextVisitNotes;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
}
