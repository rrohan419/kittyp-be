package com.kittyp.clinic.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;
import com.kittyp.common.entity.BaseEntity;

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
@Table(name = "clinic_doctor_invites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ClinicDoctorInvite extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String uuid;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "clinic_id", nullable = false)
	private Clinic clinic;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String doctorName;

	@Column(nullable = false, unique = true)
	private String token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default
	private ClinicDoctorInviteStatus status = ClinicDoctorInviteStatus.PENDING;

	@Column(nullable = false)
	private Long invitedByUserId;

	@Column(nullable = false)
	private LocalDateTime expiresAt;
}
