package com.kittyp.clinic.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.email.model.ClinicEmailUsageModel;
import com.kittyp.email.service.ClinicEmailUsageService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class ClinicEmailUsageController {
    private static final String CLINIC_OWNER = KeyConstant.IS_ROLE_CLINIC_ADMIN + " or "
            + KeyConstant.IS_ROLE_DOCTOR;

    private final ApiResponse<?> responseBuilder;
    private final ClinicService clinicService;
    private final ClinicRepository clinicRepository;
    private final ClinicEmailUsageService emailUsageService;

    @GetMapping(ApiUrl.CLINIC_EMAIL_USAGE)
    @PreAuthorize(CLINIC_OWNER)
    public ResponseEntity<SuccessResponse<ClinicEmailUsageModel>> usage(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        clinicService.requireClinicManager(uuid, email);
        Clinic clinic = clinicRepository.findByUuid(uuid);
        if (clinic == null) {
            throw new CustomException("Clinic not found", HttpStatus.NOT_FOUND);
        }
        return responseBuilder.buildSuccessResponse(
                emailUsageService.getUsage(clinic.getId(), pageNumber, pageSize),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
