package com.kittyp.clinic.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicInventoryMovement;

public interface ClinicInventoryMovementRepository extends JpaRepository<ClinicInventoryMovement, Long> {

    Page<ClinicInventoryMovement> findByClinic_IdAndIsActiveTrue(Long clinicId, Pageable pageable);
}
