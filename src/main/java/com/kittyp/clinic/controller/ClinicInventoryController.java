package com.kittyp.clinic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryMovementModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.StockAdjustmentRequest;
import com.kittyp.clinic.service.ClinicInventoryService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.PaginationModel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicInventoryController {

    private static final String CLINIC_ACCESS = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF + " or " + KeyConstant.IS_ROLE_DOCTOR;

    private final ClinicInventoryService inventoryService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.CLINIC_INVENTORY)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<PaginationModel<InventoryItemModel>>> list(@PathVariable String uuid,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(defaultValue = KeyConstant.PAGE_SIZE) Integer pageSize) {
        return success(inventoryService.list(uuid, category, pageNumber, pageSize, email()));
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_OPTIONS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<InventoryItemModel>>> options(@PathVariable String uuid,
            @RequestParam String category) {
        return success(inventoryService.options(uuid, category, email()));
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_MOVEMENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<PaginationModel<InventoryMovementModel>>> movements(
            @PathVariable String uuid,
            @RequestParam(defaultValue = KeyConstant.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(defaultValue = KeyConstant.PAGE_SIZE) Integer pageSize) {
        return success(inventoryService.movements(uuid, pageNumber, pageSize, email()));
    }

    @PostMapping(ApiUrl.CLINIC_INVENTORY)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<InventoryItemModel>> create(@PathVariable String uuid,
            @Valid @RequestBody InventoryItemRequest request) {
        return success(inventoryService.create(uuid, request, email()));
    }

    @PatchMapping(ApiUrl.CLINIC_INVENTORY_ITEM)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<InventoryItemModel>> update(@PathVariable String uuid,
            @PathVariable String itemUuid, @Valid @RequestBody InventoryItemRequest request) {
        return success(inventoryService.update(uuid, itemUuid, request, email()));
    }

    @PostMapping(ApiUrl.CLINIC_INVENTORY_STOCK)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<InventoryItemModel>> adjustStock(@PathVariable String uuid,
            @PathVariable String itemUuid, @Valid @RequestBody StockAdjustmentRequest request) {
        return success(inventoryService.adjustStock(uuid, itemUuid, request, email()));
    }

    @DeleteMapping(ApiUrl.CLINIC_INVENTORY_ITEM)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable String uuid, @PathVariable String itemUuid) {
        inventoryService.delete(uuid, itemUuid, email());
        return success(null);
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private static String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}