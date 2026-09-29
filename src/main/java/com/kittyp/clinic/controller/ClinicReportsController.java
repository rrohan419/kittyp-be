package com.kittyp.clinic.controller;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.dto.ClinicReportsDto.ReportModel;
import com.kittyp.clinic.service.ClinicReportsService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicReportsController {

    private static final String CLINIC_ACCESS = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_CLINIC_STAFF + " or " + KeyConstant.IS_ROLE_DOCTOR;

    private final ClinicReportsService clinicReportsService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.CLINIC_REPORTS)
    @PreAuthorize(CLINIC_ACCESS)
    public ResponseEntity<SuccessResponse<ReportModel>> reports(
            @org.springframework.web.bind.annotation.PathVariable String uuid,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        ReportModel report = clinicReportsService.reports(uuid, currentEmail(), from, to);
        return responseBuilder.buildSuccessResponse(report, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
