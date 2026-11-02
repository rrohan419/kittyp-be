package com.kittyp.clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dao.ClinicStaffDao;
import com.kittyp.clinic.dto.ClinicInventoryDtos.BatchReceiptRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.BatchReviewRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryBatchModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryEditAccess;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryMovementModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockAdjustmentRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockDirection;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicDoctor;
import com.kittyp.clinic.entity.ClinicInventoryBatch;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryBatchRepository;
import com.kittyp.common.util.AlphanumericIdService;
import com.kittyp.clinic.entity.ClinicStaff;
import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.clinic.entity.ClinicInventoryMovement;
import com.kittyp.clinic.enums.InventoryMovementAction;
import com.kittyp.clinic.enums.InventoryMovementSource;
import com.kittyp.clinic.enums.InventoryReservationStatus;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.clinic.repository.ClinicInventoryMovementRepository;
import com.kittyp.clinic.repository.ClinicInventoryReservationRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;
import com.kittyp.user.entity.User;
import com.kittyp.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicInventoryServiceImpl implements ClinicInventoryService {

    private final ClinicDao clinicDao;
    private final ClinicInventoryItemRepository inventoryRepository;
    private final ClinicInventoryMovementRepository movementRepository;
    private final ClinicInventoryReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ClinicStaffDao clinicStaffDao;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ClinicService clinicService;
    private final ClinicInventoryBatchRepository batchRepository;
    private final ClinicInventoryStockSupport stock;

    @Override
    @Transactional
    public PaginationModel<InventoryItemModel> list(String clinicUuid, String category, Integer pageNumber,
            Integer pageSize, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int size = PaginationSupport.clampSize(pageSize);
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("uuid")));
        String filter = blankToNull(category);
        Page<ClinicInventoryItem> items = filter == null
                ? inventoryRepository.findByClinic_IdAndIsActiveTrue(clinic.getId(), pageable)
                : inventoryRepository.findByClinic_IdAndIsActiveTrueAndCategoryIgnoreCase(clinic.getId(), filter,
                        pageable);
        List<InventoryItemModel> models = toModels(clinic, items.getContent());
        Map<String, InventoryItemModel> byUuid = models.stream()
                .collect(Collectors.toMap(InventoryItemModel::uuid, m -> m));
        return PaginationSupport.fromPage(items.map(item -> byUuid.get(item.getUuid())));
    }

    @Override
    @Transactional
    public List<InventoryItemModel> options(String clinicUuid, String category, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        String filter = blankToNull(category);
        if (filter == null) {
            throw new CustomException("Inventory category is required", HttpStatus.BAD_REQUEST);
        }
        return toModels(clinic, inventoryRepository
                .findByClinic_IdAndIsActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(clinic.getId(), filter));
    }

    @Override
    public PaginationModel<InventoryMovementModel> movements(String clinicUuid, Integer pageNumber, Integer pageSize,
            String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int size = PaginationSupport.clampSize(pageSize);
        Page<ClinicInventoryMovement> movements = movementRepository.findByClinic_IdAndIsActiveTrue(clinic.getId(),
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PaginationSupport.fromPage(movements.map(this::toMovement));
    }

    @Override
    @Transactional
    public InventoryItemModel create(String clinicUuid, InventoryItemRequest request, String email) {
        Clinic clinic = editorAccess(clinicUuid, email);
        LocalDate today = LocalDate.now();
        int opening = request.quantity() == null ? 0 : request.quantity();
        if (opening > 0) {
            ClinicInventoryStockSupport.validateExpiry(blankToNull(request.category()), request.expiryDate(),
                    request.nonExpiring(), false, today);
        }
        ClinicInventoryItem item = new ClinicInventoryItem();
        item.setClinic(clinic);
        apply(item, request, false);
        item.setQuantity(0);
        item = inventoryRepository.save(item);
        List<ClinicInventoryBatch> batches = new ArrayList<>();
        ClinicInventoryBatch batch = null;
        if (opening > 0) {
            batch = stock.receive(item, batches, request.batchNumber(), request.expiryDate(), request.nonExpiring(),
                    opening, request.unitCost(), today);
            stock.syncAggregate(item, batches);
        }
        stock.record(clinic, item, actor(email), InventoryMovementAction.OPENING, opening, opening, "Opening balance",
                InventoryMovementSource.MANUAL, null, null, batch);
        return toModel(item, batches, 0, Map.of(), 0, today);
    }

    @Override
    @Transactional
    public InventoryItemModel update(String clinicUuid, String itemUuid, InventoryItemRequest request, String email) {
        Clinic clinic = editorAccess(clinicUuid, email);
        ClinicInventoryItem item = lockedItem(itemUuid, clinic);
        boolean becameMedication = !ClinicInventoryStockSupport.isMedication(item.getCategory())
                && ClinicInventoryStockSupport.isMedication(request.category());
        apply(item, request, false);
        List<ClinicInventoryBatch> batches = stock.batches(item);
        stock.ensureLegacyBatch(item, batches);
        if (becameMedication) {
            // Medication stock must carry a real expiry date: undated/non-expiring stock goes back to review.
            for (ClinicInventoryBatch b : batches) {
                if (ClinicInventoryStockSupport.qty(b) > 0 && b.getExpiryDate() == null) {
                    b.setNonExpiring(false);
                    b.setReviewRequired(true);
                    stock.saveBatch(b);
                }
            }
        }
        item = inventoryRepository.save(item);
        return toModelWithHolds(item, batches);
    }

    @Override
    @Transactional
    public InventoryItemModel adjustStock(String clinicUuid, String itemUuid, StockAdjustmentRequest request,
            String email) {
        Clinic clinic = editorAccess(clinicUuid, email);
        ClinicInventoryItem item = lockedItem(itemUuid, clinic);
        LocalDate today = LocalDate.now();
        List<ClinicInventoryBatch> batches = stock.batches(item);
        stock.ensureLegacyBatch(item, batches);
        int units = request.quantity();
        User actor = actor(email);
        String extra = blankToNull(request.note());
        if (request.direction() == StockDirection.ADD) {
            ClinicInventoryBatch batch = stock.receive(item, batches, request.batchNumber(), request.expiryDate(),
                    request.nonExpiring(), units, request.unitCost(), today);
            stock.syncAggregate(item, batches);
            stock.record(clinic, item, actor, InventoryMovementAction.ADJUST, units,
                    ClinicInventoryStockSupport.qty(batch),
                    extra == null ? "Stock added" : "Stock added: " + extra, InventoryMovementSource.MANUAL, null, null,
                    batch);
            return toModelWithHolds(item, batches);
        }
        String label = extra == null ? "Stock removed" : "Stock removed: " + extra;
        String batchUuid = blankToNull(request.batchUuid());
        if (batchUuid != null) {
            ClinicInventoryBatch batch = batches.stream().filter(b -> batchUuid.equals(b.getUuid())).findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("clinic inventory batch", "uuid", batchUuid));
            int held = stock.heldByBatch(item).getOrDefault(batch.getId(), 0);
            int free = ClinicInventoryStockSupport.qty(batch) - held;
            if (units > free) {
                throw new CustomException("Only " + Math.max(free, 0) + " " + item.getName() + " removable from batch "
                        + batch.getBatchNumber(), HttpStatus.CONFLICT);
            }
            batch.setQuantityOnHand(ClinicInventoryStockSupport.qty(batch) - units);
            stock.saveBatch(batch);
            stock.syncAggregate(item, batches);
            stock.record(clinic, item, actor, InventoryMovementAction.ADJUST, -units,
                    ClinicInventoryStockSupport.qty(batch), trimNote(label),
                    InventoryMovementSource.MANUAL, null, null, batch);
            return toModelWithHolds(item, batches);
        }
        // No batch given: FEFO over eligible (unexpired, reviewed, unheld) stock only.
        int running = item.getQuantity() == null ? 0 : item.getQuantity();
        List<ClinicInventoryStockSupport.Slot> slots = stock.slots(item, batches, today);
        int eligibleFree = slots.stream().mapToInt(ClinicInventoryStockSupport.Slot::available).sum();
        if (units > eligibleFree) {
            ClinicInventoryStockSupport.ItemStock s = ClinicInventoryStockSupport.summarize(item, batches, today,
                    Map.of(), 0);
            throw new CustomException("Only " + eligibleFree + " eligible " + item.getName() + " available for removal"
                    + (s.expired() + s.unreviewed() > 0 ? " (" + s.expired() + " expired and " + s.unreviewed()
                            + " unreviewed units must be removed by choosing their batch)" : "") + ".",
                    HttpStatus.CONFLICT);
        }
        int left = units;
        for (ClinicInventoryStockSupport.Slot slot : slots) {
            if (left == 0) {
                break;
            }
            int take = Math.min(left, slot.available());
            ClinicInventoryBatch batch = slot.batch();
            batch.setQuantityOnHand(ClinicInventoryStockSupport.qty(batch) - take);
            stock.saveBatch(batch);
            running -= take;
            left -= take;
            stock.record(clinic, item, actor, InventoryMovementAction.ADJUST, -take,
                    ClinicInventoryStockSupport.qty(batch), trimNote(label + " (FEFO)"),
                    InventoryMovementSource.MANUAL, null, null, batch);
        }
        stock.syncAggregate(item, batches);
        return toModelWithHolds(item, batches);
    }

    @Override
    @Transactional
    public List<InventoryBatchModel> listBatches(String clinicUuid, String itemUuid, boolean includeEmpty,
            String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        ClinicInventoryItem item = lockedItem(itemUuid, clinic);
        List<ClinicInventoryBatch> batches = stock.batches(item);
        stock.ensureLegacyBatch(item, batches);
        return batchModels(item, batches, includeEmpty, LocalDate.now());
    }

    @Override
    @Transactional
    public InventoryBatchModel receiveBatch(String clinicUuid, String itemUuid, BatchReceiptRequest request,
            String email) {
        Clinic clinic = editorAccess(clinicUuid, email);
        ClinicInventoryItem item = lockedItem(itemUuid, clinic);
        List<ClinicInventoryBatch> batches = stock.batches(item);
        stock.ensureLegacyBatch(item, batches);
        ClinicInventoryBatch batch = stock.receive(item, batches, request.batchNumber(), request.expiryDate(),
                request.nonExpiring(), request.quantity(), request.unitCost(), LocalDate.now());
        stock.syncAggregate(item, batches);
        String extra = blankToNull(request.note());
        stock.record(clinic, item, actor(email), InventoryMovementAction.RECEIVE, request.quantity(),
                ClinicInventoryStockSupport.qty(batch),
                trimNote(extra == null ? "Batch received" : "Batch received: " + extra), InventoryMovementSource.MANUAL,
                null, null, batch);
        return batchModels(item, batches, true, LocalDate.now()).stream()
                .filter(m -> m.uuid().equals(batch.getUuid())).findFirst().orElse(null);
    }

    @Override
    @Transactional
    public InventoryBatchModel reviewBatch(String clinicUuid, String itemUuid, String batchUuid,
            BatchReviewRequest request, String email) {
        Clinic clinic = editorAccess(clinicUuid, email);
        ClinicInventoryItem item = lockedItem(itemUuid, clinic);
        LocalDate today = LocalDate.now();
        List<ClinicInventoryBatch> batches = stock.batches(item);
        stock.ensureLegacyBatch(item, batches);
        ClinicInventoryBatch batch = batches.stream().filter(b -> batchUuid.equals(b.getUuid())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("clinic inventory batch", "uuid", batchUuid));
        ClinicInventoryStockSupport.validateExpiry(item.getCategory(), request.expiryDate(), request.nonExpiring(), true,
                today);
        String before = describe(batch);
        String number = blankToNull(request.batchNumber());
        if (number != null && number.equalsIgnoreCase(ClinicInventoryStockSupport.LEGACY_BATCH_NUMBER)) {
            throw new CustomException("Batch number " + ClinicInventoryStockSupport.LEGACY_BATCH_NUMBER + " is reserved",
                    HttpStatus.BAD_REQUEST);
        }
        if (number == null && ClinicInventoryStockSupport.LEGACY_BATCH_NUMBER.equals(batch.getBatchNumber())) {
            number = "LEGACY-" + AlphanumericIdService.generate();
        }
        if (number != null) {
            for (ClinicInventoryBatch other : batches) {
                if (other != batch && number.equalsIgnoreCase(other.getBatchNumber())) {
                    throw new CustomException("Batch " + number + " already exists for this item", HttpStatus.CONFLICT);
                }
            }
            batch.setBatchNumber(number);
        }
        boolean nonExp = Boolean.TRUE.equals(request.nonExpiring());
        batch.setNonExpiring(nonExp);
        batch.setExpiryDate(nonExp ? null : request.expiryDate());
        batch.setReviewRequired(false);
        if (request.unitCost() != null) {
            batch.setUnitCost(request.unitCost());
        }
        stock.saveBatch(batch);
        stock.syncAggregate(item, batches);
        String extra = blankToNull(request.note());
        stock.record(clinic, item, actor(email), InventoryMovementAction.REVIEW, 0,
                ClinicInventoryStockSupport.qty(batch),
                trimNote("Batch reviewed: " + before + " -> " + describe(batch) + (extra == null ? "" : "; " + extra)),
                InventoryMovementSource.MANUAL, null, null, batch);
        return batchModels(item, batches, true, today).stream().filter(m -> m.uuid().equals(batch.getUuid()))
                .findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void delete(String clinicUuid, String itemUuid, String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = lockedItem(itemUuid, clinic);
        List<ClinicInventoryBatch> batches = stock.batches(item);
        stock.ensureLegacyBatch(item, batches);
        int onHand = item.getQuantity() == null ? 0 : item.getQuantity();
        for (ClinicInventoryBatch b : batches) {
            b.setQuantityOnHand(0);
            stock.saveBatch(b);
        }
        item.setQuantity(0);
        item.setIsActive(false);
        inventoryRepository.save(item);
        stock.record(clinic, item, actor(email), InventoryMovementAction.ADJUST, -onHand, 0, "Item removed",
                InventoryMovementSource.MANUAL, null, null, null);
    }

    private static String describe(ClinicInventoryBatch b) {
        String expiry = Boolean.TRUE.equals(b.getNonExpiring()) ? "non-expiring"
                : b.getExpiryDate() == null ? "no expiry" : "expiry " + b.getExpiryDate();
        return (b.getBatchNumber() == null ? "?" : b.getBatchNumber()) + " " + expiry
                + (Boolean.TRUE.equals(b.getReviewRequired()) ? " (unreviewed)" : "");
    }

    private ClinicInventoryItem lockedItem(String itemUuid, Clinic clinic) {
        return inventoryRepository.findActiveForUpdate(itemUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("clinic inventory item", "uuid", itemUuid));
    }

    private List<InventoryBatchModel> batchModels(ClinicInventoryItem item, List<ClinicInventoryBatch> batches,
            boolean includeEmpty, LocalDate today) {
        Map<Long, Integer> held = stock.heldByBatch(item);
        Map<Long, Integer> avail = new java.util.HashMap<>();
        stock.slots(item, batches, today).forEach(s -> avail.put(s.batch().getId(), s.available()));
        List<ClinicInventoryBatch> ordered = new ArrayList<>(batches);
        ordered.sort(ClinicInventoryStockSupport.FEFO);
        List<InventoryBatchModel> out = new ArrayList<>();
        for (ClinicInventoryBatch b : ordered) {
            if (!includeEmpty && ClinicInventoryStockSupport.qty(b) <= 0) {
                continue;
            }
            Long days = b.getExpiryDate() == null || Boolean.TRUE.equals(b.getNonExpiring()) ? null
                    : ClinicInventoryStockSupport.daysToExpiry(b, today);
            out.add(new InventoryBatchModel(b.getUuid(), item.getUuid(), b.getBatchNumber(), b.getExpiryDate(),
                    b.getNonExpiring(), b.getQuantityReceived(), b.getQuantityOnHand(),
                    held.getOrDefault(b.getId(), 0), avail.getOrDefault(b.getId(), 0), b.getUnitCost(),
                    b.getReceivedAt(), ClinicInventoryStockSupport.status(b, today).name(), b.getReviewRequired(), days));
        }
        return out;
    }
    private Clinic clinicAccess(String clinicUuid, String email) {
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        clinicService.get(clinicUuid, email);
        return clinic;
    }

    @Override
    public InventoryEditAccess editAccess(String clinicUuid, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        if (clinicService.isClinicManager(clinicUuid, email) || mayEditItems(clinic, email)) {
            return new InventoryEditAccess(true);
        }
        return new InventoryEditAccess(false);
    }

    private Clinic managerAccess(String clinicUuid, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        clinicService.requireClinicManager(clinicUuid, email);
        return clinic;
    }

    /** Clinic admin/owner, or staff/doctor with canEditInventory (items and stock). */
    private Clinic editorAccess(String clinicUuid, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        if (clinicService.isClinicManager(clinicUuid, email) || mayEditItems(clinic, email)) {
            return clinic;
        }
        throw new CustomException("You do not have permission to edit inventory items", HttpStatus.FORBIDDEN);
    }

    private boolean mayEditItems(Clinic clinic, String email) {
        User user = actor(email);
        if (user == null || user.getId() == null) {
            return false;
        }
        if (clinic.getOwner() != null && clinic.getOwner().getId().equals(user.getId())) {
            return true;
        }
        boolean staffAllowed = clinicStaffDao.findLatestByClinicAndUser(clinic.getId(), user.getId())
                .filter(row -> Boolean.TRUE.equals(row.getIsActive()))
                .map(ClinicStaff::isCanEditInventory)
                .orElse(false);
        if (staffAllowed) {
            return true;
        }
        return clinicDoctorRepository.findFirstByClinic_IdAndDoctor_User_IdAndIsActiveTrue(clinic.getId(), user.getId())
                .map(ClinicDoctor::isCanEditInventory)
                .orElse(false);
    }

    private ClinicInventoryItem item(String itemUuid, Clinic clinic) {
        return inventoryRepository.findByUuidAndClinic_IdAndIsActiveTrue(itemUuid, clinic.getId())
                .orElseThrow(() -> new ResourceNotFoundException("clinic inventory item", "uuid", itemUuid));
    }

    private static void apply(ClinicInventoryItem item, InventoryItemRequest request, boolean includeQuantity) {
        item.setName(request.name().trim());
        item.setCategory(blankToNull(request.category()));
        if (includeQuantity) {
            item.setQuantity(request.quantity());
        }
        item.setUnit(blankToNull(request.unit()));
        item.setReorderLevel(request.reorderLevel());
        item.setUnitPrice(request.unitPrice());
        if (request.active() != null) {
            item.setIsActive(request.active());
        } else if (item.getIsActive() == null) {
            item.setIsActive(true);
        }
    }

    private User actor(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    static String displayName(User user) {
        if (user == null) {
            return null;
        }
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private Map<String, Integer> heldByItem(Long clinicId) {
        return reservationRepository
                .sumActiveByClinic(clinicId, InventoryReservationStatus.ACTIVE, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(row -> String.valueOf(row[0]), row -> ((Number) row[1]).intValue()));
    }

    private List<InventoryItemModel> toModels(Clinic clinic, List<ClinicInventoryItem> items) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<ClinicInventoryBatch> all = new ArrayList<>();
        if (!items.isEmpty()) {
            all = batchRepository.findByClinic_IdAndItem_IdInAndIsActiveTrue(clinic.getId(),
                    items.stream().map(ClinicInventoryItem::getId).toList());
        }
        Map<Long, List<ClinicInventoryBatch>> byItem = all.stream()
                .collect(Collectors.groupingBy(b -> b.getItem().getId()));
        Map<Long, Integer> heldByBatch = new HashMap<>();
        for (Object[] row : reservationRepository.sumActiveByBatchForClinic(clinic.getId(),
                InventoryReservationStatus.ACTIVE, now)) {
            heldByBatch.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }
        Map<String, Integer> batchless = new HashMap<>();
        for (Object[] row : reservationRepository.sumActiveWithoutBatchForClinic(clinic.getId(),
                InventoryReservationStatus.ACTIVE, now)) {
            batchless.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
        }
        Map<String, Integer> held = heldByItem(clinic.getId());
        List<InventoryItemModel> out = new ArrayList<>();
        for (ClinicInventoryItem item : items) {
            out.add(toModel(item, byItem.getOrDefault(item.getId(), List.of()), held.getOrDefault(item.getUuid(), 0),
                    heldByBatch, batchless.getOrDefault(item.getUuid(), 0), today));
        }
        return out;
    }

    private InventoryItemModel toModelWithHolds(ClinicInventoryItem item, List<ClinicInventoryBatch> batches) {
        Map<Long, Integer> heldByBatch = stock.heldByBatch(item);
        int batchless = reservationRepository.sumActiveWithoutBatch(item.getUuid(), InventoryReservationStatus.ACTIVE,
                LocalDateTime.now());
        int reserved = heldByBatch.values().stream().mapToInt(Integer::intValue).sum() + batchless;
        return toModel(item, batches, reserved, heldByBatch, batchless, LocalDate.now());
    }

    private InventoryItemModel toModel(ClinicInventoryItem item, List<ClinicInventoryBatch> batches, int reserved,
            Map<Long, Integer> heldByBatch, int batchless, LocalDate today) {
        ClinicInventoryStockSupport.ItemStock s = ClinicInventoryStockSupport.summarize(item, batches, today,
                heldByBatch, batchless);
        return new InventoryItemModel(item.getUuid(), item.getClinic().getUuid(), item.getName(), item.getCategory(),
                item.getQuantity(), item.getUnit(), item.getReorderLevel(), item.getUnitPrice(), item.getIsActive(),
                item.getCreatedAt(), item.getUpdatedAt(), reserved, s.available(), s.eligible(), s.expired(),
                s.unreviewed(), s.expiringSoon(), s.nextExpiry(), s.expiringSoon() > 0,
                ClinicInventoryStockSupport.EXPIRY_WARNING_DAYS);
    }
    private InventoryMovementModel toMovement(ClinicInventoryMovement movement) {
        return new InventoryMovementModel(movement.getUuid(), movement.getItemUuid(), movement.getItemName(),
                movement.getAction().name(), movement.getQuantityDelta(), movement.getQuantityAfter(),
                movement.getNote(), movement.getSource().name(), movement.getInvoiceUuid(),
                movement.getInvoiceNumber(), movement.getActorName(), movement.getCreatedAt(),
                movement.getBatch() == null ? null : movement.getBatch().getUuid(),
                movement.getBatch() == null ? null : movement.getBatch().getBatchNumber());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimNote(String note) {
        return note.length() <= 500 ? note : note.substring(0, 500);
    }
}