package com.kittyp.clinic.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicInventoryItem;

import jakarta.persistence.LockModeType;

public interface ClinicInventoryItemRepository extends JpaRepository<ClinicInventoryItem, Long> {

    Optional<ClinicInventoryItem> findByUuidAndClinic_IdAndIsActiveTrue(String uuid, Long clinicId);

    List<ClinicInventoryItem> findByClinic_IdAndIsActiveTrueOrderByNameAsc(Long clinicId);

    List<ClinicInventoryItem> findTop10ByClinic_IdAndIsActiveTrueOrderByCreatedAtDesc(Long clinicId);

    List<ClinicInventoryItem> findTop10ByClinic_IdAndIsActiveTrueOrderByUpdatedAtDesc(Long clinicId);

    long countByClinic_IdAndIsActiveTrue(Long clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ClinicInventoryItem i WHERE i.uuid = :uuid AND i.clinic.id = :clinicId AND i.isActive = true")
    Optional<ClinicInventoryItem> findByUuidForUpdate(@Param("uuid") String uuid, @Param("clinicId") Long clinicId);

    Optional<ClinicInventoryItem> findFirstByClinic_IdAndBarcodeAndIsActiveTrue(Long clinicId, String barcode);

    Optional<ClinicInventoryItem> findFirstByClinic_IdAndGtinAndIsActiveTrue(Long clinicId, String gtin);

    @Query("""
            SELECT i FROM ClinicInventoryItem i
            WHERE i.clinic.id = :clinicId AND i.isActive = true
            AND (
                LOWER(i.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(i.category) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(i.barcode, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(i.manufacturer, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(i.sku, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY i.name ASC
            """)
    List<ClinicInventoryItem> searchActive(@Param("clinicId") Long clinicId, @Param("q") String q);

    @Query("""
            SELECT COALESCE(SUM(i.stock), 0) FROM ClinicInventoryItem i
            WHERE i.clinic.id = :clinicId AND i.isActive = true
            """)
    BigDecimal sumStock(@Param("clinicId") Long clinicId);

    @Query("""
            SELECT i FROM ClinicInventoryItem i
            WHERE i.clinic.id = :clinicId AND i.isActive = true
            AND i.stock <= 0
            ORDER BY i.name ASC
            """)
    List<ClinicInventoryItem> findOutOfStock(@Param("clinicId") Long clinicId);

    @Query("""
            SELECT i FROM ClinicInventoryItem i
            WHERE i.clinic.id = :clinicId AND i.isActive = true
            AND i.stock > 0 AND i.stock <= i.minStock
            ORDER BY i.stock ASC, i.name ASC
            """)
    List<ClinicInventoryItem> findLowStock(@Param("clinicId") Long clinicId);
}
