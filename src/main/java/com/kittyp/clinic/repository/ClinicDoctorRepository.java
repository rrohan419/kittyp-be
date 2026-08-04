package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicDoctor;

public interface ClinicDoctorRepository extends JpaRepository<ClinicDoctor, Long> {

    List<ClinicDoctor> findByClinic_IdAndIsActiveTrue(Long clinicId);

    boolean existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(Long clinicId, Long userId);
}
