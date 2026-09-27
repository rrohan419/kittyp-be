package com.kittyp.clinic.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.dto.ClinicInventoryDtos.AlertModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.ConsumptionRow;
import com.kittyp.clinic.dto.ClinicInventoryDtos.DashboardModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryItemRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.InventoryLotModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.MovementModel;
import com.kittyp.clinic.dto.ClinicInventoryDtos.MovementRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.ScanRequest;
import com.kittyp.clinic.dto.ClinicInventoryDtos.ScanResult;
import com.kittyp.clinic.dto.ClinicInventoryDtos.WeeklyReportModel;
import com.kittyp.clinic.service.ClinicInventoryService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.model.MessageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicInventoryController {

    private static final String CLINIC_ACCESS = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF + " or " + KeyConstant.IS_ROLE_DOCTOR;

    private final ApiResponse<?> responseBuilder;
    private final ClinicInventoryService clinicInventoryService;

    @GetMapping(ApiUrl.CLINIC_INVENTORY)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<InventoryItemModel>>> list(
            @PathVariable String uuid,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) String expiryStatus) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.list(uuid, q, stockStatus, expiryStatus, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_DASHBOARD)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<DashboardModel>> dashboard(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.dashboard(uuid, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_MOVEMENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<Page<MovementModel>>> movements(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.listMovements(uuid, page, size, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CLINIC_INVENTORY_MOVEMENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<MovementModel>> recordMovement(
            @PathVariable String uuid, @Valid @RequestBody MovementRequest request) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.recordMovement(uuid, request, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.CREATED);
    }

    @PostMapping(ApiUrl.CLINIC_INVENTORY_SCAN)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ScanResult>> scan(
            @PathVariable String uuid, @Valid @RequestBody ScanRequest request) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.scan(uuid, request, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_ALERTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<AlertModel>>> alerts(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.listAlerts(uuid, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_WEEKLY_REPORT)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<WeeklyReportModel>> weeklyReport(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.weeklyReport(uuid, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_CONSUMPTION)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<ConsumptionRow>>> consumption(
            @PathVariable String uuid, @RequestParam(defaultValue = "30") int days) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.consumption(uuid, days, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.CLINIC_INVENTORY_LOTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<InventoryLotModel>>> lots(
            @PathVariable String uuid, @PathVariable String itemUuid) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.listLots(uuid, itemUuid, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PostMapping(ApiUrl.CLINIC_INVENTORY)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<InventoryItemModel>> create(
            @PathVariable String uuid,
            @Valid @RequestBody InventoryItemRequest request) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.create(uuid, request, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.CREATED);
    }

    @PutMapping(ApiUrl.CLINIC_INVENTORY_ITEM)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<InventoryItemModel>> update(
            @PathVariable String uuid,
            @PathVariable String itemUuid,
            @Valid @RequestBody InventoryItemRequest request) {
        return responseBuilder.buildSuccessResponse(
                clinicInventoryService.update(uuid, itemUuid, request, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @DeleteMapping(ApiUrl.CLINIC_INVENTORY_ITEM)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<MessageResponse>> delete(
            @PathVariable String uuid,
            @PathVariable String itemUuid) {
        clinicInventoryService.delete(uuid, itemUuid, email());
        return responseBuilder.buildSuccessResponse(
                new MessageResponse("Inventory item removed"),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
