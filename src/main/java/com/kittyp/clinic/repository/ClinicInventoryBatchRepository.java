package com.kittyp.clinic.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.kittyp.clinic.entity.ClinicInventoryBatch;
import com.kittyp.clinic.entity.ClinicInventoryItem;

public interface ClinicInventoryBatchRepository extends JpaRepository<ClinicInventoryBatch, Long> {

    List<ClinicInventoryBatch> findByItem_IdAndIsActiveTrue(Long itemId);

    List<ClinicInventoryBatch> findByClinic_IdAndItem_IdInAndIsActiveTrue(Long clinicId, Collection<Long> itemIds);

    Optional<ClinicInventoryBatch> findByUuidAndItem_IdAndIsActiveTrue(String uuid, Long itemId);

    /** Active items whose aggregate quantity is not fully backed by batches (pre-batch legacy stock). */
    @Query("""
            SELECT i FROM ClinicInventoryItem i
            WHERE i.isActive = true AND i.quantity > COALESCE(
                (SELECT SUM(b.quantityOnHand) FROM ClinicInventoryBatch b WHERE b.item = i AND b.isActive = true), 0L)
            """)
    List<ClinicInventoryItem> findItemsWithUnbatchedQuantity();
}
