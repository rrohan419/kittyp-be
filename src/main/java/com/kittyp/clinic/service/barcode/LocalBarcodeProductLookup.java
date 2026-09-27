package com.kittyp.clinic.service.barcode;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.util.Gs1BarcodeParser.ParsedBarcode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalBarcodeProductLookup implements BarcodeProductLookup {

    private final ClinicInventoryItemRepository itemRepository;

    @Override
    public Optional<ClinicInventoryItem> findLocal(Long clinicId, ParsedBarcode parsed) {
        if (parsed == null) {
            return Optional.empty();
        }
        if (StringUtils.hasText(parsed.gtin())) {
            Optional<ClinicInventoryItem> byGtin =
                    itemRepository.findFirstByClinic_IdAndGtinAndIsActiveTrue(clinicId, parsed.gtin());
            if (byGtin.isPresent()) {
                return byGtin;
            }
            Optional<ClinicInventoryItem> byBarcodeAsGtin =
                    itemRepository.findFirstByClinic_IdAndBarcodeAndIsActiveTrue(clinicId, parsed.gtin());
            if (byBarcodeAsGtin.isPresent()) {
                return byBarcodeAsGtin;
            }
        }
        if (StringUtils.hasText(parsed.raw())) {
            return itemRepository.findFirstByClinic_IdAndBarcodeAndIsActiveTrue(clinicId, parsed.raw().trim());
        }
        return Optional.empty();
    }
}
