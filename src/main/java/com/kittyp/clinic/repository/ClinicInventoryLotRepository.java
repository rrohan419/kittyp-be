package com.kittyp.clinic.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicInventoryLot;

import jakarta.persistence.LockModeType;

public interface ClinicInventoryLotRepository extends JpaRepository<ClinicInventoryLot, Long> {

    Optional<ClinicInventoryLot> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

    List<ClinicInventoryLot> findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM ClinicInventoryLot l WHERE l.uuid = :uuid AND l.clinic.id = :clinicId AND l.isActive = true")
    Optional<ClinicInventoryLot> findByUuidForUpdate(@Param("uuid") String uuid, @Param("clinicId") Long clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM ClinicInventoryLot l
            WHERE l.inventoryItem.id = :itemId AND l.clinic.id = :clinicId AND l.isActive = true
            AND l.quantity > 0
            ORDER BY CASE WHEN l.expiresOn IS NULL THEN 1 ELSE 0 END, l.expiresOn ASC, l.id ASC
            """)
    List<ClinicInventoryLot> findFefoLotsForUpdate(@Param("itemId") Long itemId, @Param("clinicId") Long clinicId);

    @Query("""
            SELECT l FROM ClinicInventoryLot l
            WHERE l.clinic.id = :clinicId AND l.isActive = true AND l.inventoryItem.isActive = true
            AND l.expiresOn IS NOT NULL AND l.expiresOn < :today
            AND l.quantity > 0
            ORDER BY l.expiresOn ASC
            """)
    List<ClinicInventoryLot> findExpiredWithStock(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    @Query("""
            SELECT l FROM ClinicInventoryLot l
            WHERE l.clinic.id = :clinicId AND l.isActive = true AND l.inventoryItem.isActive = true
            AND l.expiresOn IS NOT NULL AND l.expiresOn >= :today AND l.expiresOn <= :soon
            AND l.quantity > 0
            ORDER BY l.expiresOn ASC
            """)
    List<ClinicInventoryLot> findExpiringSoonWithStock(
            @Param("clinicId") Long clinicId, @Param("today") LocalDate today, @Param("soon") LocalDate soon);
}
