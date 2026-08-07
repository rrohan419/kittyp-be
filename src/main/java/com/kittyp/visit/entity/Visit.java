package com.kittyp.visit.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicPetOwner;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.entity.Pet;
import com.kittyp.visit.enums.VisitSource;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.enums.VisitUrgency;

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
@Table(name = "visits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class Visit extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_owner_id")
    private ClinicPetOwner clinicOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private DoctorProfile doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitSource source;

    /** Care channel — reuse BookingMode meaning (IN_PERSON / VIDEO). Not walk-in. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingMode channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitUrgency urgency;

    @Column(name = "reason_for_visit", columnDefinition = "TEXT")
    private String reasonForVisit;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "examination_notes", columnDefinition = "TEXT")
    private String examinationNotes;

    @Column(columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "plan_notes", columnDefinition = "TEXT")
    private String plan;

    @Column(name = "next_visit_notes", columnDefinition = "TEXT")
    private String nextVisitNotes;

    /** JSON object e.g. {"weightKg":4.2,"temperatureC":38.5} */
    @Column(name = "vitals_json", columnDefinition = "TEXT")
    private String vitalsJson;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "invoice_uuid", length = 64)
    private String invoiceUuid;

    @Column(name = "health_event_uuid", length = 64)
    private String healthEventUuid;
}
