package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicPetEnrollment;

public interface ClinicPetEnrollmentRepository extends JpaRepository<ClinicPetEnrollment, Long> {

	Optional<ClinicPetEnrollment> findByClinic_IdAndPet_IdAndIsActiveTrue(Long clinicId, Long petId);

	Optional<ClinicPetEnrollment> findByClinic_IdAndPet_UuidAndIsActiveTrue(Long clinicId, String petUuid);

	List<ClinicPetEnrollment> findByClinic_IdAndIsActiveTrue(Long clinicId);

	boolean existsByClinic_IdAndPet_UuidAndIsActiveTrue(Long clinicId, String petUuid);
}
