package com.kittyp.booking.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.doctor.entity.DoctorProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "doctor_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class DoctorAvailability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @Column(columnDefinition = "TEXT")
    private String weeklyScheduleJson;

    @Column(columnDefinition = "TEXT")
    private String exceptionsJson;

    private Integer slotDurationMinutes;

    private Integer bufferMinutes;

    private String timezone;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
