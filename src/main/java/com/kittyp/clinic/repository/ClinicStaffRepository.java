package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicStaff;

public interface ClinicStaffRepository extends JpaRepository<ClinicStaff, Long> {

    boolean existsByClinic_IdAndUser_IdAndIsActiveTrue(Long clinicId, Long userId);

    boolean existsByUser_IdAndIsActiveTrue(Long userId);

    List<ClinicStaff> findByUser_IdAndIsActiveTrue(Long userId);

    List<ClinicStaff> findByClinic_IdAndIsActiveTrue(Long clinicId);

    Optional<ClinicStaff> findFirstByClinic_IdAndUser_IdOrderByIdDesc(Long clinicId, Long userId);
}
