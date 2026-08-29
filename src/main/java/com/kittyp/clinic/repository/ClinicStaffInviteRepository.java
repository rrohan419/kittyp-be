package com.kittyp.clinic.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicStaffInvite;
import com.kittyp.clinic.enums.ClinicStaffInviteStatus;

public interface ClinicStaffInviteRepository extends JpaRepository<ClinicStaffInvite, Long> {

	Optional<ClinicStaffInvite> findByToken(String token);

	Optional<ClinicStaffInvite> findByUuid(String uuid);

	List<ClinicStaffInvite> findByClinic_IdOrderByCreatedAtDesc(Long clinicId);

	Optional<ClinicStaffInvite> findByClinic_IdAndEmailIgnoreCaseAndStatus(Long clinicId, String email,
			ClinicStaffInviteStatus status);

	long countByClinic_IdAndCreatedAtAfter(Long clinicId, LocalDateTime after);
}
