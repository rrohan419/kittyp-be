package com.kittyp.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.user.entity.Pet;

public interface PetsRepository extends JpaRepository<Pet, Long> {

	void deleteByUuid(String uuid);

	Pet findByUuid(String uuid);

	Optional<Pet> findOptionalByUuid(String uuid);

	List<Pet> findByClinic_IdAndIsActiveTrue(Long clinicId);

	List<Pet> findByClinicOwner_IdAndIsActiveTrue(Long clinicOwnerId);

	List<Pet> findByClinicOwner_UuidAndIsActiveTrue(String ownerUuid);

	Optional<Pet> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

	long countByClinic_IdAndIsActiveTrue(Long clinicId);

	@Query("""
			SELECT p FROM Pet p
			WHERE p.clinic.id = :clinicId AND p.isActive = true
			AND (
				LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.breed, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.type, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.microchipNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(p.uuid) LIKE LOWER(CONCAT('%', :q, '%'))
			)
			""")
	List<Pet> searchByClinic(@Param("clinicId") Long clinicId, @Param("q") String q);
}
