package com.kittyp.clinic.service;

import java.time.LocalDateTime;
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
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryMovementModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockAdjustmentRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockDirection;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
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
    private final ClinicService clinicService;

    @Override
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
        Map<String, Integer> held = heldByItem(clinic.getId());
        return PaginationSupport.fromPage(items.map(item -> toModel(item, held.getOrDefault(item.getUuid(), 0))));
    }

    @Override
    public List<InventoryItemModel> options(String clinicUuid, String category, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        String filter = blankToNull(category);
        if (filter == null) {
            throw new CustomException("Inventory category is required", HttpStatus.BAD_REQUEST);
        }
        Map<String, Integer> held = heldByItem(clinic.getId());
        return inventoryRepository
                .findByClinic_IdAndIsActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(clinic.getId(), filter)
                .stream()
                .map(item -> toModel(item, held.getOrDefault(item.getUuid(), 0)))
                .toList();
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
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = new ClinicInventoryItem();
        item.setClinic(clinic);
        apply(item, request, true);
        item = inventoryRepository.save(item);
        record(clinic, item, actor(email), InventoryMovementAction.OPENING, item.getQuantity(), item.getQuantity(),
                "Opening balance", InventoryMovementSource.MANUAL, null, null);
        return toModel(item);
    }

    @Override
    @Transactional
    public InventoryItemModel update(String clinicUuid, String itemUuid, InventoryItemRequest request, String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = item(itemUuid, clinic);
        apply(item, request, false);
        return toModel(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public InventoryItemModel adjustStock(String clinicUuid, String itemUuid, StockAdjustmentRequest request,
            String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = item(itemUuid, clinic);
        int onHand = item.getQuantity() == null ? 0 : item.getQuantity();
        int units = request.quantity();
        boolean add = request.direction() == StockDirection.ADD;
        if (!add && units > onHand) {
            throw new CustomException("Only " + onHand + " " + item.getName() + " on hand.", HttpStatus.CONFLICT);
        }
        int delta = add ? units : -units;
        int after = onHand + delta;
        item.setQuantity(after);
        item = inventoryRepository.save(item);
        String note = add ? "Stock added" : "Stock removed";
        String extra = blankToNull(request.note());
        if (extra != null) {
            note = note + ": " + extra;
        }
        record(clinic, item, actor(email), InventoryMovementAction.ADJUST, delta, after, trimNote(note),
                InventoryMovementSource.MANUAL, null, null);
        return toModel(item);
    }

    @Override
    @Transactional
    public void delete(String clinicUuid, String itemUuid, String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = item(itemUuid, clinic);
        int onHand = item.getQuantity() == null ? 0 : item.getQuantity();
        item.setQuantity(0);
        item.setIsActive(false);
        inventoryRepository.save(item);
        record(clinic, item, actor(email), InventoryMovementAction.ADJUST, -onHand, 0, "Item removed",
                InventoryMovementSource.MANUAL, null, null);
    }

    private Clinic clinicAccess(String clinicUuid, String email) {
        Clinic clinic = clinicDao.findByUuid(clinicUuid);
        if (clinic == null) {
            throw new ResourceNotFoundException("clinic", "uuid", clinicUuid);
        }
        clinicService.get(clinicUuid, email);
        return clinic;
    }

    private Clinic managerAccess(String clinicUuid, String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        clinicService.requireClinicManager(clinicUuid, email);
        return clinic;
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

    private void record(Clinic clinic, ClinicInventoryItem item, User actor, InventoryMovementAction action, int delta,
            int after, String note, InventoryMovementSource source, String invoiceUuid, String invoiceNumber) {
        ClinicInventoryMovement movement = new ClinicInventoryMovement();
        movement.setClinic(clinic);
        movement.setItemUuid(item.getUuid());
        movement.setItemName(item.getName());
        movement.setActor(actor);
        movement.setActorName(displayName(actor));
        movement.setAction(action);
        movement.setQuantityDelta(delta);
        movement.setQuantityAfter(after);
        movement.setNote(note);
        movement.setSource(source);
        movement.setInvoiceUuid(invoiceUuid);
        movement.setInvoiceNumber(invoiceNumber);
        movement.setIsActive(true);
        movementRepository.save(movement);
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

    private InventoryItemModel toModel(ClinicInventoryItem item) {
        return toModel(item, 0);
    }

    private InventoryItemModel toModel(ClinicInventoryItem item, int reserved) {
        return new InventoryItemModel(item.getUuid(), item.getClinic().getUuid(), item.getName(), item.getCategory(),
                item.getQuantity(), item.getUnit(), item.getReorderLevel(), item.getUnitPrice(), item.getIsActive(),
                item.getCreatedAt(), item.getUpdatedAt(), reserved);
    }

    private InventoryMovementModel toMovement(ClinicInventoryMovement movement) {
        return new InventoryMovementModel(movement.getUuid(), movement.getItemUuid(), movement.getItemName(),
                movement.getAction().name(), movement.getQuantityDelta(), movement.getQuantityAfter(),
                movement.getNote(), movement.getSource().name(), movement.getInvoiceUuid(),
                movement.getInvoiceNumber(), movement.getActorName(), movement.getCreatedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimNote(String note) {
        return note.length() <= 500 ? note : note.substring(0, 500);
    }
}