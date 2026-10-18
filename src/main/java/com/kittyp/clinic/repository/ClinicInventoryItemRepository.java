package com.kittyp.clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicInventoryItem;

import jakarta.persistence.LockModeType;

public interface ClinicInventoryItemRepository extends JpaRepository<ClinicInventoryItem, Long> {

    Page<ClinicInventoryItem> findByClinic_IdAndIsActiveTrue(Long clinicId, Pageable pageable);

    Page<ClinicInventoryItem> findByClinic_IdAndIsActiveTrueAndCategoryIgnoreCase(Long clinicId, String category,
            Pageable pageable);

    List<ClinicInventoryItem> findByClinic_IdAndIsActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(Long clinicId,
            String category);

    Optional<ClinicInventoryItem> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM ClinicInventoryItem i
            WHERE i.uuid = :uuid AND i.clinic.id = :clinicId AND i.isActive = true
            """)
    Optional<ClinicInventoryItem> findActiveForUpdate(@Param("uuid") String uuid, @Param("clinicId") Long clinicId);
}