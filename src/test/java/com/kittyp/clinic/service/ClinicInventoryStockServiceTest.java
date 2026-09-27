package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryLotRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.common.exception.CustomException;

@ExtendWith(MockitoExtension.class)
class ClinicInventoryStockServiceTest {

    @Mock
    private ClinicInventoryItemRepository itemRepository;
    @Mock
    private ClinicInventoryLotRepository lotRepository;
    @Mock
    private ClinicInventoryMovementRepository movementRepository;

    @InjectMocks
    private ClinicInventoryStockService stockService;

    @Test
    void deductIsIdempotentForSameInvoiceLine() {
        Clinic clinic = Clinic.builder().build();
        clinic.setId(1L);
        ClinicInventoryItem item = ClinicInventoryItem.builder()
                .uuid("item1")
                .name("Amox")
                .stock(BigDecimal.TEN)
                .trackStock(true)
                .build();
        item.setId(10L);
        when(itemRepository.findByUuidForUpdate("item1", 1L)).thenReturn(Optional.of(item));
        when(movementRepository.existsByInvoiceUuidAndInventoryItem_IdAndInvoiceLineKey(
                        eq("inv1"), eq(10L), eq("L1")))
                .thenReturn(true);

        assertDoesNotThrow(() -> stockService.deductForInvoiceLine(
                clinic, "item1", null, BigDecimal.ONE, "inv1", "L1", null));
        verify(lotRepository, never()).findFefoLotsForUpdate(anyLong(), anyLong());
    }

    @Test
    void oversellBlocked() {
        Clinic clinic = Clinic.builder().build();
        clinic.setId(1L);
        ClinicInventoryItem item = ClinicInventoryItem.builder()
                .uuid("item1")
                .name("Amox")
                .stock(BigDecimal.valueOf(2))
                .trackStock(true)
                .clinic(clinic)
                .build();
        item.setId(10L);
        when(itemRepository.findByUuidForUpdate("item1", 1L)).thenReturn(Optional.of(item));
        when(movementRepository.existsByInvoiceUuidAndInventoryItem_IdAndInvoiceLineKey(any(), any(), any()))
                .thenReturn(false);

        assertThrows(CustomException.class, () -> stockService.deductForInvoiceLine(
                clinic, "item1", null, BigDecimal.TEN, "inv1", "L1", null));
    }
}
