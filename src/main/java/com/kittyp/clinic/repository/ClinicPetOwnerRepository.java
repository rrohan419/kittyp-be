package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.kittyp.clinic.entity.ClinicPetOwner;

public interface ClinicPetOwnerRepository extends JpaRepository<ClinicPetOwner, Long>, JpaSpecificationExecutor<ClinicPetOwner> {

	List<ClinicPetOwner> findByClinic_IdAndIsActiveTrue(Long clinicId);

	Optional<ClinicPetOwner> findByClinic_IdAndEmailIgnoreCaseAndIsActiveTrue(Long clinicId, String email);

	Optional<ClinicPetOwner> findByUuid(String uuid);

	boolean existsByUuid(String uuid);

	Optional<ClinicPetOwner> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

	long countByClinic_IdAndIsActiveTrue(Long clinicId);

	List<ClinicPetOwner> findByLinkedUserIsNullAndIsActiveTrueAndEmailIgnoreCase(String email);

	/** All active clinic owners with this email (including already linked — for reclaim). */
	List<ClinicPetOwner> findByIsActiveTrueAndEmailIgnoreCase(String email);

	List<ClinicPetOwner> findByLinkedUserIsNullAndIsActiveTrueAndPhone(String phone);

	List<ClinicPetOwner> findByLinkedUserIsNullAndIsActiveTrueAndAlternatePhone(String alternatePhone);

	List<ClinicPetOwner> findByLinkedUser_IdAndIsActiveTrue(Long linkedUserId);

	boolean existsByClinic_IdAndLinkedUser_IdAndIsActiveTrue(Long clinicId, Long linkedUserId);

	Optional<ClinicPetOwner> findByClinic_IdAndLinkedUser_IdAndIsActiveTrue(Long clinicId, Long linkedUserId);

	List<ClinicPetOwner> findByClinic_IdAndIsActiveTrueAndPhone(Long clinicId, String phone);

}
