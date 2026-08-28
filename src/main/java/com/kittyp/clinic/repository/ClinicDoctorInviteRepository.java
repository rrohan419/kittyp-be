package com.kittyp.clinic.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicDoctorInvite;
import com.kittyp.clinic.enums.ClinicDoctorInviteStatus;

public interface ClinicDoctorInviteRepository extends JpaRepository<ClinicDoctorInvite, Long> {

	Optional<ClinicDoctorInvite> findByToken(String token);

	Optional<ClinicDoctorInvite> findByUuid(String uuid);

	List<ClinicDoctorInvite> findByClinic_IdAndStatus(Long clinicId, ClinicDoctorInviteStatus status);

	List<ClinicDoctorInvite> findByClinic_IdOrderByCreatedAtDesc(Long clinicId);

	Optional<ClinicDoctorInvite> findByClinic_IdAndEmailIgnoreCaseAndStatus(Long clinicId, String email,
			ClinicDoctorInviteStatus status);

	List<ClinicDoctorInvite> findByEmailIgnoreCaseAndStatus(String email, ClinicDoctorInviteStatus status);

	long countByClinic_IdAndCreatedAtAfter(Long clinicId, LocalDateTime after);
}
