package com.kittyp.clinic.service;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kittyp.clinic.entity.ClinicInventoryBatch;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryBatchRepository;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moves pre-batch item-level quantity into a review-required LEGACY batch (visible, never billable until
 * reviewed). Idempotent; also applied lazily by every stock path via ClinicInventoryStockSupport.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicInventoryLegacyMigrationService {

    private final ClinicInventoryBatchRepository batchRepository;
    private final ClinicInventoryItemRepository itemRepository;
    private final ClinicInventoryStockSupport stock;
    private final PlatformTransactionManager transactionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateOnStartup() {
        try {
            int moved = migrateAll();
            if (moved > 0) {
                log.info("Moved {} inventory items' pre-batch stock into LEGACY review batches", moved);
            }
        } catch (Exception e) {
            log.warn("Legacy inventory migration skipped: {}", e.getMessage());
        }
    }

    public int migrateAll() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<ClinicInventoryItem> pending = tx.execute(s -> batchRepository.findItemsWithUnbatchedQuantity().stream()
                .map(i -> i).toList());
        int count = 0;
        if (pending == null) {
            return 0;
        }
        for (ClinicInventoryItem candidate : pending) {
            String uuid = candidate.getUuid();
            Long clinicId = tx.execute(s -> candidate.getClinic().getId());
            Boolean done = tx.execute(s -> {
                ClinicInventoryItem item = itemRepository.findActiveForUpdate(uuid, clinicId).orElse(null);
                if (item == null) {
                    return false;
                }
                List<ClinicInventoryBatch> batches = stock.batches(item);
                stock.ensureLegacyBatch(item, batches);
                return true;
            });
            if (Boolean.TRUE.equals(done)) {
                count++;
            }
        }
        return count;
    }
}
