package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.env.Environment;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.entity.ClinicInventoryReservation;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryReservationStatus;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.clinic.repository.ClinicInventoryReservationRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ClinicInventoryConsumptionServiceTest {

    @Mock
    private ClinicInventoryItemRepository inventoryRepository;
    @Mock
    private ClinicInventoryMovementRepository movementRepository;
    @Mock
    private ClinicInventoryReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Environment environment;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @InjectMocks
    private ClinicInventoryConsumptionService service;

    @Test
    void consumeDecrementsWholeUnitsAndWritesMovement() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(5);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 2, false));

        service.consumeForIssue(invoice);

        assertEquals(3, item.getQuantity());
        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(InventoryMovementAction.CONSUME, movement.getValue().getAction());
        assertEquals(-2, movement.getValue().getQuantityDelta());
        assertEquals(3, movement.getValue().getQuantityAfter());
        assertEquals("INV-1", movement.getValue().getInvoiceNumber());
        assertEquals("Invoice consumption", movement.getValue().getNote());
    }

    @Test
    void draftReservesWithoutReducingOnHand() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(86);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        when(reservationRepository.sumActiveQuantity(any(), any(), any())).thenReturn(0);
        when(environment.getProperty(any(), eq(Integer.class), any())).thenReturn(5);
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 50, false));

        service.reserveForDraft(invoice);

        assertEquals(86, item.getQuantity());
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository).save(any(ClinicInventoryReservation.class));
        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(InventoryMovementAction.RESERVE, movement.getValue().getAction());
        assertEquals(-50, movement.getValue().getQuantityDelta());
        assertEquals(86, movement.getValue().getQuantityAfter());
    }

    @Test
    void shortDraftDoesNotReserveOrConsume() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(40);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        when(reservationRepository.sumActiveQuantity(any(), any(), any())).thenReturn(0);
        when(environment.getProperty(any(), eq(Integer.class), any())).thenReturn(5);
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 56, false));

        service.reserveForDraft(invoice);

        assertEquals(40, item.getQuantity());
        verify(reservationRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void activeHoldConsumesWithoutAsking() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(50);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        ClinicInventoryReservation hold = ClinicInventoryReservation.builder().quantity(50)
                .status(InventoryReservationStatus.ACTIVE).build();
        when(reservationRepository.findFirstByInvoiceUuidAndItemUuidAndStatusAndExpiresAtAfter(any(), any(), any(),
                any())).thenReturn(Optional.of(hold));
        when(reservationRepository.sumActiveQuantity(any(), any(), any())).thenReturn(50);
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 50, false));

        service.consumeForIssue(invoice);

        assertEquals(0, item.getQuantity());
        assertEquals(InventoryReservationStatus.CONSUMED, hold.getStatus());
    }

    @Test
    void issueWithoutHoldAsksBeforeGoingShort() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(40);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        when(reservationRepository.sumActiveQuantity(any(), any(), any())).thenReturn(0);
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 56, false));

        CustomException ex = assertThrows(CustomException.class, () -> service.consumeForIssue(invoice));

        assertEquals(409, ex.getHttpStatus().value());
        assertEquals(40, item.getQuantity());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void shortStockIsRejectedUntilInventoryIsAdded() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(40);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        when(reservationRepository.sumActiveQuantity(any(), any(), any())).thenReturn(0);
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 56, true));

        CustomException ex = assertThrows(CustomException.class, () -> service.consumeForIssue(invoice));

        assertEquals(409, ex.getHttpStatus().value());
        assertEquals(40, item.getQuantity());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void issueDoesNotAskAgainAndDoesNotConsumeTwice() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(1);
        when(movementRepository.existsByInvoiceUuidAndActionAndIsActiveTrue("inv-1", InventoryMovementAction.CONSUME))
                .thenReturn(true);
        ConsultationInvoice invoice = invoice(clinic, line(item.getUuid(), 4, false));

        service.consumeForIssue(invoice);

        assertEquals(1, item.getQuantity());
        verify(inventoryRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void releaseActiveHoldsMarksReleasedAndWritesAudit() {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(10);
        ClinicInventoryReservation hold = ClinicInventoryReservation.builder().clinic(clinic).itemUuid(item.getUuid())
                .invoiceUuid("inv-1").invoiceNumber("INV-1").quantity(4).status(InventoryReservationStatus.ACTIVE)
                .build();
        when(reservationRepository.findByInvoiceUuidAndStatus("inv-1", InventoryReservationStatus.ACTIVE))
                .thenReturn(List.of(hold));
        when(inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(item.getUuid(), clinic.getId()))
                .thenReturn(Optional.of(item));

        service.releaseActiveHolds(invoice(clinic, "[]"));

        assertEquals(InventoryReservationStatus.RELEASED, hold.getStatus());
        ArgumentCaptor<ClinicInventoryMovement> movement = ArgumentCaptor.forClass(ClinicInventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertEquals(InventoryMovementAction.RELEASE, movement.getValue().getAction());
        assertEquals(4, movement.getValue().getQuantityDelta());
        assertEquals(10, item.getQuantity());
    }

    @Test
    void fractionalQuantityIsRejected() throws Exception {
        Clinic clinic = clinic();
        ClinicInventoryItem item = item(5);
        when(inventoryRepository.findActiveForUpdate(item.getUuid(), clinic.getId())).thenReturn(Optional.of(item));
        String json = """
                [{"itemType":"MEDICINE","description":"Amoxicillin","quantity":1.5,"unitPrice":10,"inventoryItemUuid":"%s"}]
                """.formatted(item.getUuid());
        ConsultationInvoice invoice = ConsultationInvoice.builder().uuid("inv-1").invoiceNumber("INV-1").clinic(clinic)
                .lineItems(json).build();

        assertThrows(CustomException.class, () -> service.consumeForIssue(invoice));
        verify(movementRepository, never()).save(any());
    }

    private static Clinic clinic() {
        Clinic clinic = Clinic.builder().uuid("clinic-1").name("Main").build();
        clinic.setId(10L);
        return clinic;
    }

    private static ClinicInventoryItem item(int quantity) {
        return ClinicInventoryItem.builder().uuid("item-1").name("Amoxicillin").category("MEDICATION").quantity(quantity)
                .unit("tab").reorderLevel(1).unitPrice(BigDecimal.TEN).build();
    }

    private String line(String itemUuid, int quantity, boolean allowShort) throws Exception {
        return """
                [{"itemType":"MEDICINE","description":"Amoxicillin","quantity":%d,"unitPrice":10,"inventoryItemUuid":"%s","allowShortStock":%s}]
                """.formatted(quantity, itemUuid, allowShort);
    }

    private static ConsultationInvoice invoice(Clinic clinic, String lineItems) {
        return ConsultationInvoice.builder().uuid("inv-1").invoiceNumber("INV-1").clinic(clinic).lineItems(lineItems)
                .build();
    }
}
