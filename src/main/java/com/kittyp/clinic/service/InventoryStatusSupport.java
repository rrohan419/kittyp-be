package com.kittyp.clinic.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryLot;
import com.kittyp.clinic.enums.InventoryExpiryStatus;
import com.kittyp.clinic.enums.InventoryStockStatus;

public final class InventoryStatusSupport {

    private InventoryStatusSupport() {
    }

    public static int expiringSoonDays(Clinic clinic) {
        Integer d = clinic == null ? null : clinic.getInventoryExpiringSoonDays();
        return d == null || d < 1 ? 90 : d;
    }

    public static InventoryStockStatus stockStatus(ClinicInventoryItem item) {
        BigDecimal stock = item.getStock() == null ? BigDecimal.ZERO : item.getStock();
        if (stock.compareTo(BigDecimal.ZERO) <= 0) {
            return InventoryStockStatus.OUT_OF_STOCK;
        }
        int min = item.getMinStock() == null ? 0 : item.getMinStock();
        if (min > 0 && stock.compareTo(BigDecimal.valueOf(min)) <= 0) {
            return InventoryStockStatus.LOW_STOCK;
        }
        return InventoryStockStatus.IN_STOCK;
    }

    public static InventoryExpiryStatus lotExpiryStatus(ClinicInventoryLot lot, Clinic clinic, LocalDate today) {
        if (lot == null || lot.getExpiresOn() == null) {
            return InventoryExpiryStatus.NORMAL;
        }
        LocalDate exp = lot.getExpiresOn();
        if (exp.isBefore(today)) {
            return InventoryExpiryStatus.EXPIRED;
        }
        LocalDate soon = today.plusDays(expiringSoonDays(clinic));
        if (!exp.isAfter(soon)) {
            return InventoryExpiryStatus.EXPIRING_SOON;
        }
        return InventoryExpiryStatus.NORMAL;
    }

    /** Worst expiry across lots with quantity &gt; 0 (EXPIRED &gt; EXPIRING_SOON &gt; NORMAL). */
    public static InventoryExpiryStatus itemExpiryStatus(
            List<ClinicInventoryLot> lots, Clinic clinic, LocalDate today) {
        if (lots == null || lots.isEmpty()) {
            return InventoryExpiryStatus.NORMAL;
        }
        return lots.stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(l -> lotExpiryStatus(l, clinic, today))
                .max(Comparator.comparingInt(InventoryStatusSupport::expiryRank))
                .orElse(InventoryExpiryStatus.NORMAL);
    }

    private static int expiryRank(InventoryExpiryStatus s) {
        return switch (s) {
            case EXPIRED -> 2;
            case EXPIRING_SOON -> 1;
            case NORMAL -> 0;
        };
    }

    public static LocalDate earliestExpiry(List<ClinicInventoryLot> lots) {
        if (lots == null) {
            return null;
        }
        return lots.stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(ClinicInventoryLot::getExpiresOn)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);
    }
}
