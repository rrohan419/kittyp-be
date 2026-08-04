package com.kittyp.clinic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.dto.ClinicDtos.BookingModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicRequest;
import com.kittyp.clinic.dto.ClinicDtos.DoctorModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventModel;
import com.kittyp.clinic.dto.ClinicDtos.HealthEventRequest;
import com.kittyp.clinic.dto.ClinicDtos.PatientDetailModel;
import com.kittyp.clinic.dto.ClinicDtos.PatientModel;
import com.kittyp.clinic.dto.ClinicDtos.RetentionAlertModel;
import com.kittyp.clinic.service.ClinicService;
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
public class ClinicController {

    private static final String CLINIC_ACCESS = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF + " or " + KeyConstant.IS_ROLE_DOCTOR;

    private final ClinicService clinicService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.CLINIC_MINE)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<ClinicModel>>> mine() {
        return success(clinicService.mine(email()));
    }

    @PostMapping(ApiUrl.CLINIC_BASE_URL)
    @PreAuthorize(KeyConstant.IS_ROLE_CLINIC_ADMIN)
    public ResponseEntity<SuccessResponse<ClinicModel>> create(@RequestBody @Valid ClinicRequest request) {
        return success(clinicService.create(request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_BY_UUID)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> detail(@PathVariable String uuid) {
        return success(clinicService.get(uuid, email()));
    }

    @PatchMapping(ApiUrl.CLINIC_BY_UUID)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ClinicModel>> update(@PathVariable String uuid,
            @RequestBody @Valid ClinicRequest request) {
        return success(clinicService.update(uuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_DOCTORS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<DoctorModel>>> doctors(@PathVariable String uuid) {
        return success(clinicService.doctors(uuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_PATIENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<PatientModel>>> patients(@PathVariable String uuid) {
        return success(clinicService.patients(uuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_PATIENT_DETAIL)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<PatientDetailModel>> patient(@PathVariable String uuid,
            @PathVariable String petUuid) {
        return success(clinicService.patientDetail(uuid, petUuid, email()));
    }

    @GetMapping(ApiUrl.CLINIC_BOOKINGS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<PaginationModel<BookingModel>>> bookings(@PathVariable String uuid,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return success(clinicService.bookings(uuid, status, page, size, email()));
    }

    @GetMapping(ApiUrl.CLINIC_RETENTION_ALERTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<RetentionAlertModel>>> retentionAlerts(@PathVariable String uuid) {
        return success(clinicService.retentionAlerts(uuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_RETENTION_ALERT_NOTIFY)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<Void>> notifyAlert(@PathVariable String uuid, @PathVariable String alertId) {
        clinicService.notifyAlert(uuid, alertId, email());
        return success(null);
    }

    @GetMapping(ApiUrl.CLINIC_PATIENT_HEALTH_EVENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<List<HealthEventModel>>> healthEvents(@PathVariable String uuid,
            @PathVariable String petUuid) {
        return success(clinicService.healthEvents(uuid, petUuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_PATIENT_HEALTH_EVENTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<HealthEventModel>> createHealthEvent(@PathVariable String uuid,
            @PathVariable String petUuid, @RequestBody @Valid HealthEventRequest request) {
        return success(clinicService.createHealthEvent(uuid, petUuid, request, email()));
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
