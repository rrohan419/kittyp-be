package com.kittyp.clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicInventoryDtos.BatchReceiptRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.BatchReviewRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryBatchModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockAdjustmentRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockDirection;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryAllocation;
import com.kittyp.clinic.entity.ClinicInventoryBatch;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.entity.ClinicInventoryReservation;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryReservationStatus;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.repository.ClinicInventoryAllocationRepository;
import com.kittyp.clinic.repository.ClinicInventoryBatchRepository;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.clinic.repository.ClinicInventoryReservationRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.doctor.entity.ConsultationInvoice;
import com.kittyp.user.repository.UserRepository;

/** Batch/expiry behaviour with in-memory fakes behind the repositories. */
class ClinicInventoryBatchFlowTest {

    private static final String CLINIC_UUID = "clinic-1";
    private static final String ITEM_UUID = "item-1";
    private static final String EMAIL = "owner@example.com";

    private final List<ClinicInventoryBatch> store = new ArrayList<>();
    private final List<ClinicInventoryMovement> movements = new ArrayList<>();
    private final List<ClinicInventoryAllocation> allocations = new ArrayList<>();
    private final List<ClinicInventoryReservation> reservations = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    private ClinicInventoryItemRepository itemRepo;
    private ClinicInventoryMovementRepository movementRepo;
    private ClinicInventoryReservationRepository reservationRepo;
    private ClinicInventoryServiceImpl service;
    private ClinicInventoryConsumptionService consumption;
    private ClinicService clinicService;
    private Clinic clinic;
    private ClinicInventoryItem item;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        clinic = Clinic.builder().uuid(CLINIC_UUID).name("Main").build();
        clinic.setId(10L);
        item = ClinicInventoryItem.builder().uuid(ITEM_UUID).clinic(clinic).name("Amoxicillin").category("MEDICATION")
                .quantity(0).unit("tab").reorderLevel(1).unitPrice(BigDecimal.TEN).build();
        item.setId(1L);

        itemRepo = mock(ClinicInventoryItemRepository.class);
        when(itemRepo.findActiveForUpdate(ITEM_UUID, 10L)).thenReturn(Optional.of(item));
        when(itemRepo.findByUuidAndClinic_IdAndIsActiveTrue(ITEM_UUID, 10L)).thenReturn(Optional.of(item));
        when(itemRepo.save(any(ClinicInventoryItem.class))).thenAnswer(i -> i.getArgument(0));
        when(itemRepo.findByClinic_IdAndIsActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(10L, "MEDICATION"))
                .thenAnswer(i -> List.of(item));

        ClinicInventoryBatchRepository batchRepo = mock(ClinicInventoryBatchRepository.class);
        when(batchRepo.findByItem_IdAndIsActiveTrue(1L)).thenAnswer(i -> new ArrayList<>(store));
        when(batchRepo.findByClinic_IdAndItem_IdInAndIsActiveTrue(any(), any())).thenAnswer(i -> new ArrayList<>(store));
        when(batchRepo.save(any(ClinicInventoryBatch.class))).thenAnswer(i -> {
            ClinicInventoryBatch b = i.getArgument(0);
            if (b.getId() == null) {
                b.setId(ids.incrementAndGet());
                b.setUuid("b" + b.getId());
                store.add(b);
            }
            return b;
        });
        movementRepo = mock(ClinicInventoryMovementRepository.class);
        when(movementRepo.save(any(ClinicInventoryMovement.class))).thenAnswer(i -> {
            movements.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(movementRepo.existsByInvoiceUuidAndActionAndIsActiveTrue(any(), any())).thenAnswer(i ->
                movements.stream().anyMatch(m -> i.getArgument(0).equals(m.getInvoiceUuid())
                        && i.getArgument(1) == m.getAction() && Boolean.TRUE.equals(m.getIsActive())));
        reservationRepo = mock(ClinicInventoryReservationRepository.class);
        when(reservationRepo.save(any(ClinicInventoryReservation.class))).thenAnswer(i -> {
            ClinicInventoryReservation r = i.getArgument(0);
            if (!reservations.contains(r)) {
                reservations.add(r);
            }
            return r;
        });
        ClinicInventoryAllocationRepository allocationRepo = mock(ClinicInventoryAllocationRepository.class);
        when(allocationRepo.save(any(ClinicInventoryAllocation.class))).thenAnswer(i -> {
            allocations.add(i.getArgument(0));
            return i.getArgument(0);
        });
        UserRepository userRepo = mock(UserRepository.class);
        when(userRepo.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        ClinicDao clinicDao = mock(ClinicDao.class);
        when(clinicDao.findByUuid(CLINIC_UUID)).thenReturn(clinic);
        clinicService = mock(ClinicService.class);
        when(clinicService.isClinicManager(CLINIC_UUID, EMAIL)).thenReturn(true);

        ClinicInventoryStockSupport stock = new ClinicInventoryStockSupport(batchRepo, itemRepo, reservationRepo,
                movementRepo);
        service = new ClinicInventoryServiceImpl(clinicDao, itemRepo, movementRepo, reservationRepo, userRepo,
                mock(ClinicStaffDao.class), mock(ClinicDoctorRepository.class), clinicService, batchRepo, stock);
        Environment env = mock(Environment.class);
        when(env.getProperty(any(), org.mockito.ArgumentMatchers.eq(Integer.class), any(Integer.class)))
                .thenReturn(5);
        consumption = new ClinicInventoryConsumptionService(itemRepo, movementRepo, reservationRepo, userRepo,
                new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false), env, stock,
                allocationRepo);
    }

    private InventoryBatchModel receive(String number, LocalDate expiry, Boolean nonExpiring, int qty) {
        return service.receiveBatch(CLINIC_UUID, ITEM_UUID,
                new BatchReceiptRequest(number, expiry, nonExpiring, qty, new BigDecimal("2.50"), null), EMAIL);
    }

    private ClinicInventoryBatch seed(String number, LocalDate expiry, boolean nonExpiring, int qty, boolean review) {
        ClinicInventoryBatch b = ClinicInventoryBatch.builder().clinic(clinic).item(item).batchNumber(number)
                .expiryDate(expiry).nonExpiring(nonExpiring).reviewRequired(review).quantityReceived(qty)
                .quantityOnHand(qty).unitCost(BigDecimal.ONE).receivedAt(LocalDateTime.now()).build();
        b.setId(ids.incrementAndGet());
        b.setUuid("b" + b.getId());
        store.add(b);
        item.setQuantity(item.getQuantity() + qty);
        return b;
    }

    private static ConsultationInvoice invoice(Clinic clinic, int qty) {
        String json = "[{\"itemType\":\"MEDICINE\",\"description\":\"Amoxicillin\",\"quantity\":" + qty
                + ",\"unitPrice\":10,\"inventoryItemUuid\":\"" + ITEM_UUID + "\"}]";
        return ConsultationInvoice.builder().uuid("inv-1").invoiceNumber("INV-1").clinic(clinic).lineItems(json)
                .build();
    }

    @Test
    void receiveEnforcesExpiryRules() {
        assertEquals(400, assertThrows(CustomException.class, () -> receive("A", null, null, 5)).getHttpStatus().value());
        assertEquals(400, assertThrows(CustomException.class, () -> receive("A", null, true, 5)).getHttpStatus().value());
        assertEquals(400, assertThrows(CustomException.class, () -> receive("A", today.minusDays(1), null, 5))
                .getHttpStatus().value());
        assertTrue(store.isEmpty());

        item.setCategory("SUPPLY"); // other categories may be explicitly non-expiring
        InventoryBatchModel ok = receive("N1", null, true, 5);
        assertEquals("ELIGIBLE", ok.status());
        assertEquals(400, assertThrows(CustomException.class, () -> receive("N2", null, null, 5)).getHttpStatus().value());
    }

    @Test
    void inventoryMutationRequiresEditorAccess() {
        when(clinicService.isClinicManager(CLINIC_UUID, "viewer@example.com")).thenReturn(false);

        assertEquals(403, assertThrows(CustomException.class, () -> service.receiveBatch(CLINIC_UUID, ITEM_UUID,
                new BatchReceiptRequest("B1", today.plusDays(20), null, 2, BigDecimal.ONE, null),
                "viewer@example.com")).getHttpStatus().value());
    }

    @Test
    void receivingForUnknownItemReturnsNotFound() {
        when(itemRepo.findActiveForUpdate(ITEM_UUID, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.receiveBatch(CLINIC_UUID, ITEM_UUID,
                new BatchReceiptRequest("B1", today.plusDays(20), null, 2, BigDecimal.ONE, null), EMAIL));
    }

    @Test
    void receiveUpdatesAggregateAndLinksMovement() {
        InventoryBatchModel m = receive("L1", today.plusDays(30), null, 8);
        assertEquals("EXPIRING_SOON", m.status());
        assertEquals(8, item.getQuantity());
        ClinicInventoryMovement mv = movements.get(movements.size() - 1);
        assertEquals(InventoryMovementAction.RECEIVE, mv.getAction());
        assertEquals("L1", mv.getBatch().getBatchNumber());
        // same batch number with different expiry is refused, same expiry accumulates
        assertEquals(409, assertThrows(CustomException.class, () -> receive("L1", today.plusDays(300), null, 1))
                .getHttpStatus().value());
        receive("L1", today.plusDays(30), null, 2);
        assertEquals(10, item.getQuantity());
        assertEquals(1, store.size());
    }

    @Test
    void createWithOpeningStockRequiresExpiry() {
        assertThrows(CustomException.class, () -> service.create(CLINIC_UUID,
                new InventoryItemRequest("Gloves", "MEDICATION", 5, "pcs", 1, BigDecimal.ONE, true), EMAIL));
        InventoryItemModel model = service.create(CLINIC_UUID, new InventoryItemRequest("Gloves", "MEDICATION", 5,
                "pcs", 1, BigDecimal.ONE, true, "G1", today.plusDays(200), null, null), EMAIL);
        assertEquals(5, model.quantity());
        assertEquals(1, store.size());
    }

    @Test
    void updateKeepsQuantityBatchBacked() {
        seed("B1", today.plusDays(200), false, 5, false);

        InventoryItemModel updated = service.update(CLINIC_UUID, ITEM_UUID,
                new InventoryItemRequest("Updated name", "MEDICATION", 0, "capsule", 2, BigDecimal.TEN, true), EMAIL);

        assertEquals("Updated name", updated.name());
        assertEquals(5, updated.quantity());
        assertEquals(5, store.get(0).getQuantityOnHand());
    }

    @Test
    void deleteZerosBatchesAndSoftDeletesItem() {
        ClinicInventoryBatch batch = seed("B1", today.plusDays(200), false, 5, false);

        service.delete(CLINIC_UUID, ITEM_UUID, EMAIL);

        assertEquals(0, item.getQuantity());
        assertEquals(0, batch.getQuantityOnHand());
        assertEquals(false, item.getIsActive());
    }

    @Test
    void removalWithoutBatchIsFefoOverEligibleOnly() {
        ClinicInventoryBatch late = seed("LATE", today.plusDays(200), false, 5, false);
        ClinicInventoryBatch soon = seed("SOON", today.plusDays(10), false, 3, false);
        ClinicInventoryBatch expired = seed("OLD", today.minusDays(1), false, 4, false);

        service.adjustStock(CLINIC_UUID, ITEM_UUID, new StockAdjustmentRequest(StockDirection.REMOVE, 4, null), EMAIL);

        assertEquals(0, soon.getQuantityOnHand());
        assertEquals(4, late.getQuantityOnHand());
        assertEquals(4, expired.getQuantityOnHand());
        assertEquals(8, item.getQuantity());
        CustomException ex = assertThrows(CustomException.class, () -> service.adjustStock(CLINIC_UUID, ITEM_UUID,
                new StockAdjustmentRequest(StockDirection.REMOVE, 5, null), EMAIL));
        assertEquals(409, ex.getHttpStatus().value());
    }

    @Test
    void expiredBatchCanBeWrittenOffByUuid() {
        ClinicInventoryBatch expired = seed("OLD", today.minusDays(1), false, 4, false);
        service.adjustStock(CLINIC_UUID, ITEM_UUID, new StockAdjustmentRequest(StockDirection.REMOVE, 4, "disposal",
                expired.getUuid(), null, null, null, null), EMAIL);
        assertEquals(0, expired.getQuantityOnHand());
        assertEquals(0, item.getQuantity());
        assertEquals(expired, movements.get(0).getBatch());
    }

    @Test
    void expiredBatchWriteOffStillRespectsActiveReservations() {
        ClinicInventoryBatch expired = seed("OLD", today.minusDays(1), false, 4, false);
        when(reservationRepo.sumActiveByBatchForItem(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] { expired.getId(), 3 }));

        assertEquals(409, assertThrows(CustomException.class, () -> service.adjustStock(CLINIC_UUID, ITEM_UUID,
                new StockAdjustmentRequest(StockDirection.REMOVE, 2, "disposal", expired.getUuid(), null, null, null,
                        null), EMAIL)).getHttpStatus().value());
        service.adjustStock(CLINIC_UUID, ITEM_UUID,
                new StockAdjustmentRequest(StockDirection.REMOVE, 1, "disposal", expired.getUuid(), null, null, null,
                        null), EMAIL);
        assertEquals(3, expired.getQuantityOnHand());
        assertEquals(3, movements.get(0).getQuantityAfter());
    }

    @Test
    void legacyStockIsVisibleUnbillableAndReviewable() {
        item.setQuantity(10); // pre-batch item-level quantity, no batches
        InventoryItemModel before = service.options(CLINIC_UUID, "MEDICATION", EMAIL).get(0);
        assertEquals(10, before.quantity());
        assertEquals(10, before.unreviewedQuantity());
        assertEquals(0, before.availableQuantity());

        List<InventoryBatchModel> batches = service.listBatches(CLINIC_UUID, ITEM_UUID, false, EMAIL);
        assertEquals(1, batches.size());
        assertEquals("UNREVIEWED", batches.get(0).status());
        assertEquals(0, batches.get(0).availableQuantity());

        CustomException ex = assertThrows(CustomException.class, () -> consumption.consumeForIssue(invoice(clinic, 1)));
        assertEquals(409, ex.getHttpStatus().value());
        assertEquals(10, item.getQuantity());

        assertThrows(CustomException.class, () -> service.reviewBatch(CLINIC_UUID, ITEM_UUID, batches.get(0).uuid(),
                new BatchReviewRequest("X", null, true, null, null), EMAIL)); // medication needs a date
        InventoryBatchModel reviewed = service.reviewBatch(CLINIC_UUID, ITEM_UUID, batches.get(0).uuid(),
                new BatchReviewRequest("R1", today.plusDays(365), null, null, "counted"), EMAIL);
        assertEquals("ELIGIBLE", reviewed.status());
        assertEquals(10, reviewed.quantityOnHand());
        assertEquals(InventoryMovementAction.REVIEW, movements.get(movements.size() - 1).getAction());

        consumption.consumeForIssue(invoice(clinic, 4));
        assertEquals(6, item.getQuantity());
    }

    @Test
    void consumeAllocatesFefoSkippingExpiredAndSyncsAggregate() {
        ClinicInventoryBatch expired = seed("OLD", today.minusDays(2), false, 9, false);
        ClinicInventoryBatch soon = seed("SOON", today.plusDays(5), false, 2, false);
        ClinicInventoryBatch late = seed("LATE", today.plusDays(400), false, 6, false);

        consumption.consumeForIssue(invoice(clinic, 5));

        assertEquals(9, expired.getQuantityOnHand());
        assertEquals(0, soon.getQuantityOnHand());
        assertEquals(3, late.getQuantityOnHand());
        assertEquals(12, item.getQuantity());
        assertEquals(2, allocations.size());
        assertEquals(0, allocations.get(0).getInvoiceLineIndex());
        assertEquals(soon, allocations.get(0).getBatch());
        assertEquals(2, allocations.get(0).getQuantity());
        assertTrue(movements.stream().allMatch(m -> m.getBatch() != null));

        CustomException ex = assertThrows(CustomException.class, () -> consumption.consumeForIssue(invoiceWithId(4)));
        assertEquals(409, ex.getHttpStatus().value()); // only 3 eligible left, expired never used
        assertEquals(12, item.getQuantity());
    }

    private ConsultationInvoice invoiceWithId(int qty) {
        ConsultationInvoice inv = invoice(clinic, qty);
        inv.setUuid("inv-2");
        return inv;
    }

    @Test
    void reserveUsesOnlyEligibleBatchesFefo() {
        seed("OLD", today.minusDays(2), false, 9, false);
        ClinicInventoryBatch soon = seed("SOON", today.plusDays(5), false, 2, false);
        ClinicInventoryBatch late = seed("LATE", today.plusDays(400), false, 6, false);

        consumption.reserveForDraft(invoice(clinic, 4));

        assertEquals(2, reservations.size());
        assertEquals(soon, reservations.get(0).getBatch());
        assertEquals(2, reservations.get(0).getQuantity());
        assertEquals(late, reservations.get(1).getBatch());
        assertEquals(2, reservations.get(1).getQuantity());

        reservations.clear();
        consumption.reserveForDraft(invoiceWithId(9)); // 8 eligible only; expired stock not counted
        assertTrue(reservations.isEmpty());
    }

    @Test
    void invoiceHoldIsConsumedOnceWithoutReducingStockAtReservationTime() {
        ClinicInventoryBatch batch = seed("B1", today.plusDays(200), false, 5, false);
        ConsultationInvoice invoice = invoice(clinic, 2);
        when(reservationRepo.findByInvoiceUuidAndItemUuidAndStatus("inv-1", ITEM_UUID,
                InventoryReservationStatus.ACTIVE)).thenAnswer(i -> reservations.stream()
                        .filter(r -> r.getStatus() == InventoryReservationStatus.ACTIVE).toList());

        consumption.reserveForDraft(invoice);
        assertEquals(5, batch.getQuantityOnHand());
        assertEquals(1, reservations.size());
        assertEquals(batch, reservations.get(0).getBatch());

        consumption.consumeForIssue(invoice);
        consumption.consumeForIssue(invoice);

        assertEquals(InventoryReservationStatus.CONSUMED, reservations.get(0).getStatus());
        assertEquals(3, batch.getQuantityOnHand());
        assertEquals(3, item.getQuantity());
        assertEquals(1, allocations.size());
    }

    @Test
    void releaseActiveHoldWritesReleaseMovement() {
        ClinicInventoryReservation hold = ClinicInventoryReservation.builder().clinic(clinic).itemUuid(ITEM_UUID)
                .invoiceUuid("inv-release").quantity(2).expiresAt(LocalDateTime.now().plusMinutes(5))
                .status(InventoryReservationStatus.ACTIVE).build();
        hold.setIsActive(true);
        when(reservationRepo.findByInvoiceUuidAndStatus("inv-release", InventoryReservationStatus.ACTIVE))
                .thenReturn(List.of(hold));
        ConsultationInvoice invoice = invoice(clinic, 2);
        invoice.setUuid("inv-release");

        consumption.releaseActiveHolds(invoice);

        assertEquals(InventoryReservationStatus.RELEASED, hold.getStatus());
        assertEquals(InventoryMovementAction.RELEASE, movements.get(0).getAction());
    }

    @Test
    void fractionalInvoiceQuantityIsRejected() {
        seed("B1", today.plusDays(200), false, 5, false);
        ConsultationInvoice invoice = invoice(clinic, 1);
        invoice.setLineItems("[{\"itemType\":\"MEDICINE\",\"description\":\"Amoxicillin\",\"quantity\":1.5,"
                + "\"unitPrice\":10,\"inventoryItemUuid\":\"" + ITEM_UUID + "\"}]");

        assertEquals(400, assertThrows(CustomException.class, () -> consumption.consumeForIssue(invoice))
                .getHttpStatus().value());
        assertEquals(5, item.getQuantity());
        assertTrue(allocations.isEmpty());
    }

    @Test
    void optionsExposeExpiryBreakdown() {
        seed("OLD", today.minusDays(2), false, 9, false);
        seed("SOON", today.plusDays(5), false, 2, false);
        seed("LATE", today.plusDays(400), false, 6, false);
        InventoryItemModel m = service.options(CLINIC_UUID, "MEDICATION", EMAIL).get(0);
        assertEquals(17, m.quantity());
        assertEquals(8, m.eligibleQuantity());
        assertEquals(8, m.availableQuantity());
        assertEquals(9, m.expiredQuantity());
        assertEquals(2, m.expiringSoonQuantity());
        assertEquals(today.plusDays(5), m.nextExpiryDate());
        assertTrue(m.expiringSoon());
    }

    @Test
    void listPreservesPaginationAndAddsStockSummary() {
        when(itemRepo.findAll(org.mockito.ArgumentMatchers.<Specification<ClinicInventoryItem>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(item)));

        var page = service.list(CLINIC_UUID, null, "amox", 1, 20, EMAIL);

        assertEquals(1, page.getPageNumber());
        assertEquals(1L, page.getTotalElements());
        assertEquals(ITEM_UUID, page.getModels().get(0).uuid());
        assertEquals(0, page.getModels().get(0).availableQuantity());
    }
}
