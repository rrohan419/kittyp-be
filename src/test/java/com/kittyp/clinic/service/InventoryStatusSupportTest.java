package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryLot;
import com.kittyp.clinic.enums.InventoryExpiryStatus;
import com.kittyp.clinic.enums.InventoryStockStatus;

class InventoryStatusSupportTest {

    @Test
    void stockStatusLowOutAndIn() {
        ClinicInventoryItem item = ClinicInventoryItem.builder()
                .stock(BigDecimal.valueOf(8))
                .minStock(10)
                .build();
        assertEquals(InventoryStockStatus.LOW_STOCK, InventoryStatusSupport.stockStatus(item));

        item.setStock(BigDecimal.ZERO);
        assertEquals(InventoryStockStatus.OUT_OF_STOCK, InventoryStatusSupport.stockStatus(item));

        item.setStock(BigDecimal.valueOf(20));
        assertEquals(InventoryStockStatus.IN_STOCK, InventoryStatusSupport.stockStatus(item));
    }

    @Test
    void expiryStatusUsesClinicThreshold() {
        Clinic clinic = Clinic.builder().inventoryExpiringSoonDays(90).build();
        LocalDate today = LocalDate.of(2026, 9, 27);
        ClinicInventoryLot soon = ClinicInventoryLot.builder()
                .expiresOn(today.plusDays(20))
                .quantity(BigDecimal.TEN)
                .build();
        assertEquals(
                InventoryExpiryStatus.EXPIRING_SOON,
                InventoryStatusSupport.lotExpiryStatus(soon, clinic, today));

        ClinicInventoryLot expired = ClinicInventoryLot.builder()
                .expiresOn(today.minusDays(1))
                .quantity(BigDecimal.ONE)
                .build();
        assertEquals(
                InventoryExpiryStatus.EXPIRED,
                InventoryStatusSupport.lotExpiryStatus(expired, clinic, today));

        assertEquals(
                InventoryExpiryStatus.EXPIRED,
                InventoryStatusSupport.itemExpiryStatus(List.of(soon, expired), clinic, today));
    }
}
