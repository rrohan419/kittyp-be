package com.kittyp.clinic.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.InventoryMovementType;

public interface ClinicInventoryMovementRepository extends JpaRepository<ClinicInventoryMovement, Long> {

    Page<ClinicInventoryMovement> findByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(Long clinicId, Pageable pageable);

    boolean existsByInvoiceUuidAndInventoryItem_IdAndInvoiceLineKey(
            String invoiceUuid, Long inventoryItemId, String invoiceLineKey);

    @Query("""
            SELECT m.inventoryItem.uuid, m.inventoryItem.name, SUM(m.quantity)
            FROM ClinicInventoryMovement m
            WHERE m.clinic.id = :clinicId AND m.isActive = true
            AND m.type = :type
            AND m.createdAt >= :from AND m.createdAt < :to
            GROUP BY m.inventoryItem.uuid, m.inventoryItem.name
            ORDER BY SUM(m.quantity) DESC
            """)
    List<Object[]> sumQuantityByItem(
            @Param("clinicId") Long clinicId,
            @Param("type") InventoryMovementType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(m.quantity), 0) FROM ClinicInventoryMovement m
            WHERE m.clinic.id = :clinicId AND m.isActive = true
            AND m.type = :type
            AND m.createdAt >= :from AND m.createdAt < :to
            """)
    java.math.BigDecimal sumQuantity(
            @Param("clinicId") Long clinicId,
            @Param("type") InventoryMovementType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    Optional<ClinicInventoryMovement> findFirstByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(Long clinicId);

    List<ClinicInventoryMovement> findTop20ByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(Long clinicId);
}
