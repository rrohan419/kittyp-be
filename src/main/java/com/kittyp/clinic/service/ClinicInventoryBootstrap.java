package com.kittyp.clinic.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryLotRepository;

import lombok.RequiredArgsConstructor;

/**
 * Local/dev safety: ensure new inventory columns exist (Hibernate often skips NOT NULL
 * adds on non-empty tables), then backfill DEFAULT lots. DDL runs outside a Spring
 * transaction so a later JPA failure cannot roll back column adds.
 */
@Component
@RequiredArgsConstructor
public class ClinicInventoryBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClinicInventoryBootstrap.class);

    private final JdbcTemplate jdbcTemplate;
    private final ClinicInventoryItemRepository itemRepository;
    private final ClinicInventoryLotRepository lotRepository;
    private final ClinicInventoryStockService stockService;

    @Override
    public void run(ApplicationArguments args) {
        ensureSchema();
        backfillLots();
    }

    private void ensureSchema() {
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN stock TYPE numeric(19,3) USING stock::numeric");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS unit varchar(40) DEFAULT 'pcs'");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS min_stock integer DEFAULT 0");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS track_stock boolean DEFAULT true");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS barcode varchar(64)");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS gtin varchar(64)");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS sku varchar(64)");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS manufacturer varchar(200)");
        safeExec("ALTER TABLE clinic_inventory_items ADD COLUMN IF NOT EXISTS purchase_price numeric(19,2)");
        safeExec("UPDATE clinic_inventory_items SET unit = 'pcs' WHERE unit IS NULL");
        safeExec("UPDATE clinic_inventory_items SET min_stock = 0 WHERE min_stock IS NULL");
        safeExec("UPDATE clinic_inventory_items SET track_stock = true WHERE track_stock IS NULL");
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN unit SET DEFAULT 'pcs'");
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN min_stock SET DEFAULT 0");
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN track_stock SET DEFAULT true");
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN unit SET NOT NULL");
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN min_stock SET NOT NULL");
        safeExec("ALTER TABLE clinic_inventory_items ALTER COLUMN track_stock SET NOT NULL");
        safeExec("ALTER TABLE clinics ADD COLUMN IF NOT EXISTS inventory_expiring_soon_days integer DEFAULT 90");
    }

    private void backfillLots() {
        try {
            List<ClinicInventoryItem> items = itemRepository.findAll().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getIsActive()))
                    .toList();
            int created = 0;
            for (ClinicInventoryItem item : items) {
                if (lotRepository
                        .findByInventoryItem_IdAndIsActiveTrueOrderByExpiresOnAscIdAsc(item.getId())
                        .isEmpty()) {
                    stockService.ensureDefaultLot(item);
                    created++;
                }
            }
            if (created > 0) {
                log.info("Backfilled DEFAULT lots for {} inventory items", created);
            }
        } catch (Exception e) {
            log.warn("Inventory lot backfill skipped: {}", e.getMessage());
        }
    }

    private void safeExec(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("inventory schema step skipped: {} — {}", sql, e.getMessage());
        }
    }
}
