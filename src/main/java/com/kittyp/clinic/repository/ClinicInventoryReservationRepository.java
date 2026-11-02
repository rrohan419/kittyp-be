package com.kittyp.clinic.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kittyp.clinic.entity.ClinicInventoryReservation;
import com.kittyp.clinic.enums.InventoryReservationStatus;

public interface ClinicInventoryReservationRepository extends JpaRepository<ClinicInventoryReservation, Long> {

    boolean existsByInvoiceUuidAndStatusAndExpiresAtAfter(String invoiceUuid, InventoryReservationStatus status,
            LocalDateTime now);

    List<ClinicInventoryReservation> findByInvoiceUuidAndStatus(String invoiceUuid, InventoryReservationStatus status);

    Optional<ClinicInventoryReservation> findFirstByInvoiceUuidAndItemUuidAndStatusAndExpiresAtAfter(
            String invoiceUuid, String itemUuid, InventoryReservationStatus status, LocalDateTime now);

    List<ClinicInventoryReservation> findByStatusAndExpiresAtBefore(InventoryReservationStatus status,
            LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0) FROM ClinicInventoryReservation r
            WHERE r.itemUuid = :itemUuid AND r.status = :status AND r.expiresAt > :now
            """)
    int sumActiveQuantity(@Param("itemUuid") String itemUuid, @Param("status") InventoryReservationStatus status,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT r.itemUuid, COALESCE(SUM(r.quantity), 0) FROM ClinicInventoryReservation r
            WHERE r.clinic.id = :clinicId AND r.status = :status AND r.expiresAt > :now
            GROUP BY r.itemUuid
            """)
    List<Object[]> sumActiveByClinic(@Param("clinicId") Long clinicId, @Param("status") InventoryReservationStatus status,
            @Param("now") LocalDateTime now);

    List<ClinicInventoryReservation> findByInvoiceUuidAndItemUuidAndStatus(String invoiceUuid, String itemUuid,
            InventoryReservationStatus status);

    /** Active holds on an item grouped by batch: [batchId, sum]. Item-level (batchless) holds excluded. */
    @Query("""
            SELECT r.batch.id, COALESCE(SUM(r.quantity), 0) FROM ClinicInventoryReservation r
            WHERE r.itemUuid = :itemUuid AND r.batch IS NOT NULL AND r.status = :status AND r.expiresAt > :now
            GROUP BY r.batch.id
            """)
    List<Object[]> sumActiveByBatchForItem(@Param("itemUuid") String itemUuid,
            @Param("status") InventoryReservationStatus status, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(r.quantity), 0) FROM ClinicInventoryReservation r
            WHERE r.itemUuid = :itemUuid AND r.batch IS NULL AND r.status = :status AND r.expiresAt > :now
            """)
    int sumActiveWithoutBatch(@Param("itemUuid") String itemUuid, @Param("status") InventoryReservationStatus status,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT r.batch.id, COALESCE(SUM(r.quantity), 0) FROM ClinicInventoryReservation r
            WHERE r.clinic.id = :clinicId AND r.batch IS NOT NULL AND r.status = :status AND r.expiresAt > :now
            GROUP BY r.batch.id
            """)
    List<Object[]> sumActiveByBatchForClinic(@Param("clinicId") Long clinicId,
            @Param("status") InventoryReservationStatus status, @Param("now") LocalDateTime now);

    @Query("""
            SELECT r.itemUuid, COALESCE(SUM(r.quantity), 0) FROM ClinicInventoryReservation r
            WHERE r.clinic.id = :clinicId AND r.batch IS NULL AND r.status = :status AND r.expiresAt > :now
            GROUP BY r.itemUuid
            """)
    List<Object[]> sumActiveWithoutBatchForClinic(@Param("clinicId") Long clinicId,
            @Param("status") InventoryReservationStatus status, @Param("now") LocalDateTime now);
}