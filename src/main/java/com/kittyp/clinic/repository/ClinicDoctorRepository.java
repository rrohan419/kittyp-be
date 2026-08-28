package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kittyp.clinic.entity.ClinicDoctor;

public interface ClinicDoctorRepository extends JpaRepository<ClinicDoctor, Long> {

    List<ClinicDoctor> findByClinic_IdAndIsActiveTrue(Long clinicId);

    java.util.Optional<ClinicDoctor> findByClinic_IdAndDoctor_Uuid(Long clinicId, String doctorUuid);

    boolean existsByClinic_IdAndDoctor_User_IdAndIsActiveTrue(Long clinicId, Long userId);

    boolean existsByDoctor_IdAndIsActiveTrue(Long doctorId);

    @Query("select distinct cd.doctor.id from ClinicDoctor cd where cd.isActive = true")
    Set<Long> findActiveAffiliatedDoctorIds();

    List<ClinicDoctor> findByDoctor_User_IdAndIsActiveTrue(Long userId);
}
