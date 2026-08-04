package com.kittyp.clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicStaff;

public interface ClinicStaffRepository extends JpaRepository<ClinicStaff, Long> {

    boolean existsByClinic_IdAndUser_IdAndIsActiveTrue(Long clinicId, Long userId);

    List<ClinicStaff> findByUser_IdAndIsActiveTrue(Long userId);
}
