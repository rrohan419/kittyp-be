package com.kittyp.email.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kittyp.email.entity.ClinicEmailActivity;
import com.kittyp.email.entity.ClinicEmailActivity.Status;

public interface ClinicEmailActivityRepository extends JpaRepository<ClinicEmailActivity, Long> {
    long countByClinic_IdAndPeriodStartAndStatusIn(Long clinicId, LocalDate periodStart, List<Status> statuses);

    long countByClinic_IdAndPeriodStartAndStatus(Long clinicId, LocalDate periodStart, Status status);

    Page<ClinicEmailActivity> findAllByClinic_IdOrderByCreatedAtDesc(Long clinicId, Pageable pageable);
}
