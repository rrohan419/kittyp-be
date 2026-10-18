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
}
