package com.kittyp.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.doctor.entity.DoctorReview;

public interface DoctorReviewRepository extends JpaRepository<DoctorReview, Long> {

    Optional<DoctorReview> findByVisit_Uuid(String visitUuid);

    boolean existsByVisit_Uuid(String visitUuid);

    List<DoctorReview> findByDoctor_IdAndIsActiveTrue(Long doctorId);
}
