package com.kittyp.booking.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.booking.enums.BookingMode;
import com.kittyp.booking.enums.BookingStatus;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.entity.Pet;
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
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class Booking extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    @Column(nullable = false)
    private LocalDateTime slotStart;

    @Column(nullable = false)
    private LocalDateTime slotEnd;

    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    private BigDecimal price;

    @Column(length = 255)
    private String paymentId;

    @Column(length = 255)
    private String refundId;

    @Column(length = 255)
    private String jitsiRoomId;

    @Column(length = 1024)
    private String videoJoinUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 255)
    private String prescriptionId;

    @Column(length = 1024)
    private String invoiceUrl;
}
