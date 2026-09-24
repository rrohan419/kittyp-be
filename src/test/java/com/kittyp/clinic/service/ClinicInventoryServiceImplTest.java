package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ClinicInventoryServiceImplTest {

    private static final String CLINIC_UUID = "clinic-1";
    private static final String ITEM_UUID = "item-1";
    private static final String EMAIL = "owner@example.com";

    @Mock
    private ClinicDao clinicDao;
    @Mock
    private ClinicInventoryItemRepository inventoryRepository;
    @Mock
    private ClinicService clinicService;

    @InjectMocks
    private ClinicInventoryServiceImpl inventoryService;

    @Test
    void createUsesClinicFromPathAndDelegatesManagerAuthorization() {
        Clinic clinic = clinic();
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(inventoryRepository.save(any(ClinicInventoryItem.class))).thenAnswer(invocation -> {
            ClinicInventoryItem item = invocation.getArgument(0);
            item.setUuid(ITEM_UUID);
            return item;
        });

        var model = inventoryService.create(CLINIC_UUID,
                new InventoryItemRequest("  Syringes ", "SUPPLY", 12, "pcs", 5, new BigDecimal("1.25"), true),
                EMAIL);

        verify(clinicService).get(CLINIC_UUID, EMAIL);
        verify(clinicService).requireClinicManager(CLINIC_UUID, EMAIL);
        assertEquals(ITEM_UUID, model.uuid());
        assertEquals("Syringes", model.name());
        assertEquals(clinic.getUuid(), model.clinicUuid());
        assertEquals(new BigDecimal("1.25"), model.unitPrice());
    }

    @Test
    void deleteSoftDeletesOnlyActiveItemInClinic() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = ClinicInventoryItem.builder().uuid(ITEM_UUID).clinic(clinic).name("Gloves")
                .quantity(4).reorderLevel(2).unitPrice(BigDecimal.ONE).build();
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.of(item));

        inventoryService.delete(CLINIC_UUID, ITEM_UUID, EMAIL);

        assertEquals(false, item.getIsActive());
        verify(inventoryRepository).save(item);
    }

    @Test
    void updateRejectsMissingItemWithoutSaving() {
        Clinic clinic = clinic();
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> inventoryService.update(CLINIC_UUID, ITEM_UUID,
                new InventoryItemRequest("Gloves", "SUPPLY", 4, "boxes", 2, BigDecimal.ONE, true), EMAIL));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void listUsesRepositoryPageAndReturnsPaginationMetadata() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = ClinicInventoryItem.builder().uuid(ITEM_UUID).clinic(clinic).name("Gloves")
            .quantity(4).reorderLevel(2).unitPrice(BigDecimal.ONE).build();
        item.setIsActive(true);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(inventoryRepository.findByClinic_IdAndIsActiveTrue(eq(clinic.getId()), any(Pageable.class)))
            .thenReturn(new PageImpl<>(java.util.List.of(item), PageRequest.of(1, 2), 3));

        var page = inventoryService.list(CLINIC_UUID, 2, 2, EMAIL);

        assertEquals(3L, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getPageNumber());
        assertEquals(2, page.getPageSize());
        assertEquals(ITEM_UUID, page.getModels().get(0).uuid());
    }

    private static Clinic clinic() {
        Clinic clinic = Clinic.builder().uuid(CLINIC_UUID).name("Main Clinic").status(ClinicStatus.VERIFIED).build();
        clinic.setId(10L);
        return clinic;
    }
}