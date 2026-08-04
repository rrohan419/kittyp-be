package com.kittyp.clinic.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.clinic.enums.ClinicStatus;
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

    @Column(columnDefinition = "TEXT")
    private String profileImageUrl;
}
