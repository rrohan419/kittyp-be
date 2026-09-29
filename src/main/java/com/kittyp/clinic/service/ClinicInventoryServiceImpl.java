package com.kittyp.clinic.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.clinic.dao.ClinicDao;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.entity.ClinicInventoryItem;
import com.kittyp.clinic.repository.ClinicInventoryItemRepository;
import com.kittyp.common.exception.ResourceNotFoundException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.PaginationSupport;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClinicInventoryServiceImpl implements ClinicInventoryService {

    private final ClinicDao clinicDao;
    private final ClinicInventoryItemRepository inventoryRepository;
    private final ClinicService clinicService;

    @Override
    public PaginationModel<InventoryItemModel> list(String clinicUuid, Integer pageNumber, Integer pageSize,
            String email) {
        Clinic clinic = clinicAccess(clinicUuid, email);
        int page = pageNumber == null || pageNumber < 1 ? 1 : pageNumber;
        int size = PaginationSupport.clampSize(pageSize);
        Page<ClinicInventoryItem> items = inventoryRepository.findByClinic_IdAndIsActiveTrue(clinic.getId(),
                PageRequest.of(page - 1, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("uuid"))));
        return PaginationSupport.fromPage(items.map(this::toModel));
    }

    @Override
    @Transactional
    public InventoryItemModel create(String clinicUuid, InventoryItemRequest request, String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = new ClinicInventoryItem();
        item.setClinic(clinic);
        apply(item, request);
        return toModel(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public InventoryItemModel update(String clinicUuid, String itemUuid, InventoryItemRequest request, String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = item(itemUuid, clinic);
        apply(item, request);
        return toModel(inventoryRepository.save(item));
    }

    @Override
    @Transactional
    public void delete(String clinicUuid, String itemUuid, String email) {
        Clinic clinic = managerAccess(clinicUuid, email);
        ClinicInventoryItem item = item(itemUuid, clinic);
        item.setIsActive(false);
        inventoryRepository.save(item);
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

    private static void apply(ClinicInventoryItem item, InventoryItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(blankToNull(request.category()));
        item.setQuantity(request.quantity());
        item.setUnit(blankToNull(request.unit()));
        item.setReorderLevel(request.reorderLevel());
        item.setUnitPrice(request.unitPrice());
        if (request.active() != null) {
            item.setIsActive(request.active());
        } else if (item.getIsActive() == null) {
            item.setIsActive(true);
        }
    }

    private InventoryItemModel toModel(ClinicInventoryItem item) {
        return new InventoryItemModel(item.getUuid(), item.getClinic().getUuid(), item.getName(), item.getCategory(),
                item.getQuantity(), item.getUnit(), item.getReorderLevel(), item.getUnitPrice(), item.getIsActive(),
                item.getCreatedAt(), item.getUpdatedAt());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}