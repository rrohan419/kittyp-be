package com.kittyp.clinic.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.clinic.entity.ClinicInventoryItem;

public interface ClinicInventoryItemRepository extends JpaRepository<ClinicInventoryItem, Long> {

    Page<ClinicInventoryItem> findByClinic_IdAndIsActiveTrue(Long clinicId, Pageable pageable);

    Optional<ClinicInventoryItem> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);
}