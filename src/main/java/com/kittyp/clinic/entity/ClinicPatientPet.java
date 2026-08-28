package com.kittyp.clinic.entity;

import java.time.LocalDate;

import org.hibernate.annotations.DynamicUpdate;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pet registered under a clinic pet owner (clinic-scoped patient).
 * {@code globalPetId} is the portable medical identity across clinics.
 */
@Entity
@Table(name = "clinic_patient_pets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class ClinicPatientPet extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String uuid;

	/** Globally unique portable pet identity (UUID). */
	@Column(name = "global_pet_id", unique = true)
	private String globalPetId;

	/** Optional clinic-local patient number. */
	@Column(name = "patient_number")
	private String patientNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "clinic_id", nullable = false)
	private Clinic clinic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private ClinicPetOwner owner;

	@Column(nullable = false)
	private String name;

	private String species;

	private String breed;

	private String gender;

	private LocalDate dateOfBirth;

	private String weight;

	@Column(name = "microchip_number")
	private String microchipNumber;

	@Column(name = "photo_url", columnDefinition = "TEXT")
	private String photoUrl;

	private LocalDate registeredAt;

	@PrePersist
	void ensureGlobalPetId() {
		if (globalPetId == null || globalPetId.isBlank()) {
			globalPetId = java.util.UUID.randomUUID().toString();
		}
	}

	public String resolveGlobalPetId() {
		return globalPetId != null && !globalPetId.isBlank() ? globalPetId : uuid;
	}
}
