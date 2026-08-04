package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    DoctorProfile findByUser_Id(Long userId);

    Optional<DoctorProfile> findByUuid(String uuid);

    List<DoctorProfile> findAllByOrderBySubmittedAtDesc();

    List<DoctorProfile> findByStatusOrderBySubmittedAtDesc(DoctorStatus status);
}
