package com.kittyp.visit.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.dto.VisitDtos.VisitPatchRequest;
import com.kittyp.visit.dto.VisitDtos.WalkInCreateRequest;
import com.kittyp.visit.enums.VisitStatus;
import com.kittyp.visit.service.VisitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicVisitController {

    private static final String CLINIC_VISIT_ACCESS = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF + " or " + KeyConstant.IS_ROLE_DOCTOR;

    private final VisitService visitService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.CLINIC_VISITS)
    @PreAuthorize(CLINIC_VISIT_ACCESS)
    public ResponseEntity<SuccessResponse<List<VisitModel>>> list(
            @PathVariable String uuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) String doctorUuid) {
        return success(visitService.listClinicVisits(uuid, date, status, doctorUuid, email()));
    }

    @PostMapping(ApiUrl.CLINIC_VISITS_WALK_IN)
    @PreAuthorize(CLINIC_VISIT_ACCESS)
    public ResponseEntity<SuccessResponse<VisitModel>> walkIn(
            @PathVariable String uuid,
            @Valid @RequestBody WalkInCreateRequest request) {
        return responseBuilder.buildSuccessResponse(
                visitService.createWalkIn(uuid, request, email()),
                ResponseMessage.SUCCESS,
                HttpStatus.CREATED);
    }

    @PatchMapping(ApiUrl.CLINIC_VISIT_BY_UUID)
    @PreAuthorize(CLINIC_VISIT_ACCESS)
    public ResponseEntity<SuccessResponse<VisitModel>> patch(
            @PathVariable String uuid,
            @PathVariable String visitUuid,
            @RequestBody VisitPatchRequest request) {
        return success(visitService.patchVisit(uuid, visitUuid, request, email()));
    }

    @GetMapping(ApiUrl.CLINIC_PATIENT_VISITS)
    @PreAuthorize(CLINIC_VISIT_ACCESS)
    public ResponseEntity<SuccessResponse<List<VisitModel>>> patientVisits(
            @PathVariable String uuid,
            @PathVariable String petUuid) {
        return success(visitService.listPetVisitsForClinic(uuid, petUuid, email()));
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
