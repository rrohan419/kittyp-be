package com.kittyp.clinic.service.barcode;

import java.util.Optional;

import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.util.Gs1BarcodeParser.ParsedBarcode;

/**
 * Extension point for external product databases. Current impl is local-only.
 */
public interface BarcodeProductLookup {

    Optional<ClinicInventoryItem> findLocal(Long clinicId, ParsedBarcode parsed);

    /** Future: enrich from external catalog without rewriting inventory flows. */
    default Optional<ExternalProductHint> findExternal(ParsedBarcode parsed) {
        return Optional.empty();
    }

    record ExternalProductHint(String name, String manufacturer, String gtin) {
    }
}
