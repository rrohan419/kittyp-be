package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.doctor.entity.DoctorPatientEnrollment;

public interface DoctorPatientEnrollmentRepository extends JpaRepository<DoctorPatientEnrollment, Long> {

	Optional<DoctorPatientEnrollment> findByDoctor_IdAndPet_IdAndIsActiveTrue(Long doctorId, Long petId);

	Optional<DoctorPatientEnrollment> findByDoctor_IdAndPet_UuidAndIsActiveTrue(Long doctorId, String petUuid);

	List<DoctorPatientEnrollment> findByDoctor_IdAndIsActiveTrue(Long doctorId);

	boolean existsByDoctor_IdAndPet_UuidAndIsActiveTrue(Long doctorId, String petUuid);
}
