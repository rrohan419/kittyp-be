package com.kittyp.email.entity;

import java.time.LocalDate;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "clinic_email_activity", indexes = {
        @Index(name = "idx_clinic_email_activity_period", columnList = "clinic_id, period_start, status"),
        @Index(name = "idx_clinic_email_activity_history", columnList = "clinic_id, created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
public class ClinicEmailActivity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "recipient_email", nullable = false, length = 320)
    private String recipientEmail;

    @Column(name = "email_type", nullable = false, length = 160)
    private String emailType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "provider_request_id", length = 160)
    private String providerRequestId;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    public enum Status {
        RESERVED,
        ACCEPTED,
        FAILED,
        BLOCKED
    }
}
