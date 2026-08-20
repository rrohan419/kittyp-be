package com.kittyp.clinic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.dto.ClinicDtos.ClinicModel;
import com.kittyp.clinic.dto.ClinicDtos.ClinicStatusUpdateRequest;
import com.kittyp.clinic.service.ClinicService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AdminClinicController {

    private final ClinicService clinicService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.ADMIN_CLINICS)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<List<ClinicModel>>> list() {
        return responseBuilder.buildSuccessResponse(clinicService.listAllClinics(), ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.ADMIN_CLINIC_BY_UUID)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<ClinicModel>> detail(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(clinicService.getByUuidForAdmin(uuid), ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PatchMapping(ApiUrl.ADMIN_CLINIC_STATUS)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<ClinicModel>> updateStatus(@PathVariable String uuid,
            @Valid @RequestBody ClinicStatusUpdateRequest request) {
        return responseBuilder.buildSuccessResponse(clinicService.updateStatusForAdmin(uuid, request.status()),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
