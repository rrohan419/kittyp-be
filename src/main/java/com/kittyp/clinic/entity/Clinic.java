package com.kittyp.clinic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kittyp.common.crypto.EncryptedStringConverter;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.user.entity.User;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "clinics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class Clinic extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String uuid;

    private String name;

    private String licenseNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    /** Normalized city/area for discovery when GPS coords are missing. */
    @Column(length = 120)
    private String city;

    /** WGS84 latitude for nearby ranking (optional). */
    private Double latitude;

    /** WGS84 longitude for nearby ranking (optional). */
    private Double longitude;

    private String phone;

    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicStatus status;

    private String timezone;

    @Column(columnDefinition = "TEXT")
    private String operatingHours;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    /** Meta WhatsApp Cloud API Phone Number ID for this clinic's sender (never mixed with doctor). */
    @Column(name = "whatsapp_phone_number_id", length = 64)
    private String whatsappPhoneNumberId;

    /** Meta WhatsApp Business Account (WABA) ID for this clinic. */
    @Column(name = "whatsapp_business_account_id", length = 64)
    private String whatsappBusinessAccountId;

    /** Meta permanent token for this clinic's WABA number (write-only via API; encrypted at rest). */
    @JsonIgnore
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "whatsapp_token", columnDefinition = "TEXT")
    private String whatsappToken;
}
