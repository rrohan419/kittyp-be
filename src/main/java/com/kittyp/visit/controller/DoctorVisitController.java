package com.kittyp.visit.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.visit.dto.VisitDtos.AttendedPatientModel;
import com.kittyp.visit.dto.VisitDtos.VisitChartRequest;
import com.kittyp.visit.dto.VisitDtos.VisitModel;
import com.kittyp.visit.service.VisitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DoctorVisitController {

    private final VisitService visitService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.DOCTOR_VISITS_MINE)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<List<VisitModel>>> mine(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String clinicUuid) {
        if (from != null || to != null) {
            return success(visitService.listMyDoctorVisitsRange(from, to, clinicUuid, email()));
        }
        return success(visitService.listMyDoctorVisits(date, clinicUuid, email()));
    }

    @GetMapping(ApiUrl.DOCTOR_ATTENDED_PATIENTS)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<List<AttendedPatientModel>>> attendedPatients() {
        return success(visitService.listMyAttendedPatients(email()));
    }

    @PostMapping(ApiUrl.DOCTOR_VISIT_START)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<VisitModel>> start(@PathVariable String visitUuid) {
        return success(visitService.startVisit(visitUuid, email()));
    }

    @PutMapping(ApiUrl.DOCTOR_VISIT_CHART)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<VisitModel>> chart(@PathVariable String visitUuid,
            @RequestBody VisitChartRequest request) {
        return success(visitService.saveChart(visitUuid, request, email()));
    }

    @PostMapping(ApiUrl.DOCTOR_VISIT_COMPLETE)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<VisitModel>> complete(@PathVariable String visitUuid) {
        return success(visitService.completeVisit(visitUuid, email()));
    }

    @PostMapping(ApiUrl.DOCTOR_VISIT_RETURN)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<VisitModel>> returnToReception(@PathVariable String visitUuid) {
        return success(visitService.returnToReception(visitUuid, email()));
    }

    private String email() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
