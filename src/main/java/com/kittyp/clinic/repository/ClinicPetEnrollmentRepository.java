package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicPetEnrollment;

public interface ClinicPetEnrollmentRepository extends JpaRepository<ClinicPetEnrollment, Long> {

	Optional<ClinicPetEnrollment> findByClinic_IdAndPet_IdAndIsActiveTrue(Long clinicId, Long petId);

	Optional<ClinicPetEnrollment> findByClinic_IdAndPet_Id(Long clinicId, Long petId);

	Optional<ClinicPetEnrollment> findByClinic_IdAndPet_UuidAndIsActiveTrue(Long clinicId, String petUuid);

	List<ClinicPetEnrollment> findByClinic_IdAndIsActiveTrue(Long clinicId);

	List<ClinicPetEnrollment> findByPet_IdAndIsActiveTrue(Long petId);

	boolean existsByClinic_IdAndPet_UuidAndIsActiveTrue(Long clinicId, String petUuid);

	@Query("""
			SELECT COUNT(e) FROM ClinicPetEnrollment e
			WHERE e.clinic.id = :clinicId AND e.isActive = true AND e.pet.isActive = true
			""")
	long countActivePetsByClinicId(@Param("clinicId") Long clinicId);

	@Query("""
			SELECT COUNT(e) FROM ClinicPetEnrollment e
			WHERE e.clinic.id = :clinicId AND e.isActive = true AND e.pet.isActive = true
			AND e.createdAt BETWEEN :from AND :to
			""")
	long countActivePetsCreatedByClinicBetween(
			@Param("clinicId") Long clinicId,
			@Param("from") java.time.LocalDateTime from,
			@Param("to") java.time.LocalDateTime to);
}
