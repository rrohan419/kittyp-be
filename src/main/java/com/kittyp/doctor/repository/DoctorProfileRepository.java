package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    DoctorProfile findByUser_Id(Long userId);

    @EntityGraph(attributePaths = { "user", "clinic" })
    Optional<DoctorProfile> findByUuid(String uuid);

    boolean existsByUuid(String uuid);

    @EntityGraph(attributePaths = { "user", "clinic" })
    @Query("select p from DoctorProfile p")
    List<DoctorProfile> findAllWithUserAndClinic();

    @EntityGraph(attributePaths = { "user", "clinic" })
    @Query("select p from DoctorProfile p where p.status = :status")
    List<DoctorProfile> findByStatusWithUserAndClinic(@Param("status") DoctorStatus status);

    List<DoctorProfile> findAllByOrderBySubmittedAtDesc();

    List<DoctorProfile> findByStatusOrderBySubmittedAtDesc(DoctorStatus status);
}
