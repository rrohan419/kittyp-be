package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicPatientPet;

public interface ClinicPatientPetRepository extends JpaRepository<ClinicPatientPet, Long> {

	List<ClinicPatientPet> findByClinic_IdAndIsActiveTrue(Long clinicId);

	Optional<ClinicPatientPet> findByUuid(String uuid);

	Optional<ClinicPatientPet> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

	Optional<ClinicPatientPet> findByGlobalPetId(String globalPetId);

	List<ClinicPatientPet> findByOwner_UuidAndIsActiveTrue(String ownerUuid);

	List<ClinicPatientPet> findByOwner_IdAndIsActiveTrue(Long ownerId);

	long countByClinic_IdAndIsActiveTrue(Long clinicId);

	long countByOwner_IdAndIsActiveTrue(Long ownerId);

	@Query("""
			SELECT p FROM ClinicPatientPet p
			WHERE p.clinic.id = :clinicId AND p.isActive = true
			AND (
				LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.breed, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.species, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.microchipNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.globalPetId, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(p.uuid) LIKE LOWER(CONCAT('%', :q, '%'))
			)
			""")
	List<ClinicPatientPet> searchByClinic(@Param("clinicId") Long clinicId, @Param("q") String q);
}
