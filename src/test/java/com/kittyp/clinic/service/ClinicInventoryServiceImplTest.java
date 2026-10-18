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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockAdjustmentRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockDirection;
import com.kittyp.common.exception.CustomException;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.ClinicStatus;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryMovementSource;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.clinic.repository.ClinicInventoryReservationRepository;
import com.kittyp.user.repository.UserRepository;

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
    private ClinicInventoryMovementRepository movementRepository;
    @Mock
    private ClinicInventoryReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClinicStaffDao clinicStaffDao;
    @Mock
    private ClinicDoctorRepository clinicDoctorRepository;
    @Mock
    private ClinicService clinicService;

    @InjectMocks
    private ClinicInventoryServiceImpl inventoryService;

    @Test
    void createUsesClinicFromPathAndDelegatesManagerAuthorization() {
        Clinic clinic = clinic();
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicService.isClinicManager(CLINIC_UUID, EMAIL)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(ClinicInventoryItem.class))).thenAnswer(invocation -> {
            ClinicInventoryItem item = invocation.getArgument(0);
            item.setUuid(ITEM_UUID);
            return item;
        });

        var model = inventoryService.create(CLINIC_UUID,
                new InventoryItemRequest("  Syringes ", "SUPPLY", 12, "pcs", 5, new BigDecimal("1.25"), true),
                EMAIL);

        verify(clinicService).get(CLINIC_UUID, EMAIL);
        verify(clinicService).isClinicManager(CLINIC_UUID, EMAIL);
        assertEquals(ITEM_UUID, model.uuid());
        assertEquals("Syringes", model.name());
        assertEquals(clinic.getUuid(), model.clinicUuid());
        assertEquals(new BigDecimal("1.25"), model.unitPrice());

        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(InventoryMovementAction.OPENING, movement.getValue().getAction());
        assertEquals(InventoryMovementSource.MANUAL, movement.getValue().getSource());
        assertEquals(12, movement.getValue().getQuantityDelta());
        assertEquals(12, movement.getValue().getQuantityAfter());
        assertEquals(ITEM_UUID, movement.getValue().getItemUuid());
    }

    @Test
    void updateKeepsOnHandQuantity() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = ClinicInventoryItem.builder().uuid(ITEM_UUID).clinic(clinic).name("Gloves")
                .quantity(4).reorderLevel(2).unitPrice(BigDecimal.ONE).build();
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicService.isClinicManager(CLINIC_UUID, EMAIL)).thenReturn(true);
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(ClinicInventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var model = inventoryService.update(CLINIC_UUID, ITEM_UUID,
                new InventoryItemRequest("Gloves", "SUPPLY", 9, "boxes", 2, BigDecimal.ONE, true), EMAIL);

        assertEquals(4, model.quantity());
        assertEquals(4, item.getQuantity());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void addStockRecordsPositiveDelta() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = stockItem(4);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicService.isClinicManager(CLINIC_UUID, EMAIL)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(ClinicInventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.adjustStock(CLINIC_UUID, ITEM_UUID,
                new StockAdjustmentRequest(StockDirection.ADD, 3, "delivery"), EMAIL);

        assertEquals(7, item.getQuantity());
        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(InventoryMovementAction.ADJUST, movement.getValue().getAction());
        assertEquals(3, movement.getValue().getQuantityDelta());
        assertEquals(7, movement.getValue().getQuantityAfter());
        assertEquals("Stock added: delivery", movement.getValue().getNote());
    }

    @Test
    void removeStockRecordsNegativeDelta() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = stockItem(4);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicService.isClinicManager(CLINIC_UUID, EMAIL)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(ClinicInventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.adjustStock(CLINIC_UUID, ITEM_UUID,
                new StockAdjustmentRequest(StockDirection.REMOVE, 2, null), EMAIL);

        assertEquals(2, item.getQuantity());
        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(-2, movement.getValue().getQuantityDelta());
        assertEquals(2, movement.getValue().getQuantityAfter());
        assertEquals("Stock removed", movement.getValue().getNote());
    }

    @Test
    void removeStockRejectsMoreThanOnHand() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = stockItem(1);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(clinicService.isClinicManager(CLINIC_UUID, EMAIL)).thenReturn(true);
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.of(item));

        CustomException ex = assertThrows(CustomException.class, () -> inventoryService.adjustStock(CLINIC_UUID,
                ITEM_UUID, new StockAdjustmentRequest(StockDirection.REMOVE, 3, null), EMAIL));

        assertEquals(409, ex.getHttpStatus().value());
        assertEquals(1, item.getQuantity());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesOnlyActiveItemInClinic() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = ClinicInventoryItem.builder().uuid(ITEM_UUID).clinic(clinic).name("Gloves")
                .quantity(4).reorderLevel(2).unitPrice(BigDecimal.ONE).build();
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, clinic.getId()))
                .thenReturn(Optional.of(item));

        inventoryService.delete(CLINIC_UUID, ITEM_UUID, EMAIL);

        assertEquals(false, item.getIsActive());
        assertEquals(0, item.getQuantity());
        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(-4, movement.getValue().getQuantityDelta());
        assertEquals(0, movement.getValue().getQuantityAfter());
        assertEquals("Item removed", movement.getValue().getNote());
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

        var page = inventoryService.list(CLINIC_UUID, null, 2, 2, EMAIL);

        assertEquals(3L, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getPageNumber());
        assertEquals(2, page.getPageSize());
        assertEquals(ITEM_UUID, page.getModels().get(0).uuid());
    }

    private static ClinicInventoryItem stockItem(int quantity) {
        return ClinicInventoryItem.builder().uuid(ITEM_UUID).clinic(clinic()).name("Gloves").quantity(quantity)
                .reorderLevel(2).unitPrice(BigDecimal.ONE).build();
    }

    private static Clinic clinic() {
        Clinic clinic = Clinic.builder().uuid(CLINIC_UUID).name("Main Clinic").status(ClinicStatus.VERIFIED).build();
        clinic.setId(10L);
        return clinic;
    }
}