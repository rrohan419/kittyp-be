package com.kittyp.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.kittyp.user.entity.Pet;
import jakarta.persistence.LockModeType;

public interface PetsRepository extends JpaRepository<Pet, Long> {

	void deleteByUuid(String uuid);

	Pet findByUuid(String uuid);

	boolean existsByUuid(String uuid);

	Optional<Pet> findOptionalByUuid(String uuid);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Pet p WHERE p.uuid = :uuid")
	Optional<Pet> findForWeightUpdate(@Param("uuid") String uuid);

	List<Pet> findByClinic_IdAndIsActiveTrue(Long clinicId);

	List<Pet> findByClinicOwner_IdAndIsActiveTrue(Long clinicOwnerId);

	List<Pet> findByClinicOwner_UuidAndIsActiveTrue(String ownerUuid);

	Optional<Pet> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

	/** Includes soft-hidden pets so visit/history deep links still resolve. */
	Optional<Pet> findByUuidAndClinic_Id(String uuid, Long clinicId);

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

	/** Appointment picker: pet UUID, patient number, owner email, owner/user UUID — not name or phone. */
	@Query("""
			SELECT p FROM Pet p
			LEFT JOIN p.clinicOwner o
			WHERE p.clinic.id = :clinicId AND p.isActive = true
			AND (
				LOWER(p.uuid) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.patientNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(p.parentUserUuid, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(o.uuid, '')) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(COALESCE(o.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
			)
			""")
	List<Pet> searchByClinicEmailOrId(@Param("clinicId") Long clinicId, @Param("q") String q);

	Optional<Pet> findByUuidIgnoreCase(String uuid);

	Optional<Pet> findFirstByClinic_IdAndPatientNumberIgnoreCase(Long clinicId, String patientNumber);

	List<Pet> findByParentUserUuid(String parentUserUuid);

	/** Both mappings use pets.user_uuid; do not infer ownership from clinic records. */
	@Query("""
			SELECT p FROM Pet p
			WHERE p.parentUserUuid = :userUuid OR EXISTS (
				SELECT u.id FROM User u
				WHERE u.uuid = :userUuid AND p MEMBER OF u.pets
			)
			""")
	List<Pet> findPlatformPetsOfUser(@Param("userUuid") String userUuid);
}
