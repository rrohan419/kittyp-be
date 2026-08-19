package com.kittyp.doctor.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.common.crypto.EncryptedStringConverter;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.common.entity.HasPublicId;
import com.kittyp.common.entity.PublicIdEntityListener;
import com.kittyp.common.enums.DoctorSpecialization;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.user.entity.User;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "doctor_profiles")
@EntityListeners(PublicIdEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class DoctorProfile extends BaseEntity implements HasPublicId {

    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String licenseNumber;

    /** Mandatory veterinary registration / council number. */
    private String registrationNumber;

    private String phoneNumber;

    /** Meta WhatsApp Cloud API Phone Number ID for this doctor's personal sender. */
    @Column(name = "whatsapp_phone_number_id", length = 64)
    private String whatsappPhoneNumberId;

    /** Meta WhatsApp Business Account (WABA) ID. */
    @Column(name = "whatsapp_business_account_id", length = 64)
    private String whatsappBusinessAccountId;

    /** Meta permanent token for this doctor's WABA number (write-only via API; encrypted at rest). */
    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "whatsapp_token", columnDefinition = "TEXT")
    private String whatsappToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DoctorSpecialization specialization;

    private Double experienceYears;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ElementCollection
    @CollectionTable(name = "doctor_languages", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "language")
    private Set<String> languages;

    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;

    private BigDecimal consultationFee;

    private BigDecimal followUpFee;

    private String currency;

    private Double rating;

    private Integer reviewsCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DoctorStatus status;

    @Column(columnDefinition = "TEXT")
    private String licenseDocumentUrl;

    @Column(columnDefinition = "TEXT")
    private String degreeCertificateUrl;

    @Column(columnDefinition = "TEXT")
    private String registrationCertificateUrl;

    @Column(columnDefinition = "TEXT")
    private String governmentIdUrl;

    /** Comma-separated clinic photo URLs. */
    @Column(columnDefinition = "TEXT")
    private String clinicPhotosUrls;

    @Builder.Default
    private boolean emailOtpVerified = false;

    @Builder.Default
    private boolean phoneOtpVerified = false;

    // Admin verification checklist
    @Builder.Default
    private boolean checkMobileOtp = false;
    @Builder.Default
    private boolean checkEmailOtp = false;
    @Builder.Default
    private boolean checkGovernmentId = false;
    @Builder.Default
    private boolean checkDegree = false;
    @Builder.Default
    private boolean checkRegistrationCertificate = false;
    @Builder.Default
    private boolean checkClinicAddress = false;
    @Builder.Default
    private boolean checkRegistrationNumber = false;
    @Builder.Default
    private boolean checkGoogleMapsMatch = false;
    @Builder.Default
    private boolean checkClinicPhotos = false;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;
}
