package com.kittyp.doctor.smartphrase.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.auth.util.SecurityContextUtils;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.doctor.smartphrase.dto.DoctorSmartPhraseDtos.SmartPhraseModel;
import com.kittyp.doctor.smartphrase.dto.DoctorSmartPhraseDtos.SmartPhraseRequest;
import com.kittyp.doctor.smartphrase.service.DoctorSmartPhraseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DoctorSmartPhraseController {

    private final DoctorSmartPhraseService service;
    private final SecurityContextUtils securityContextUtils;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.DOCTOR_SMART_PHRASES)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<List<SmartPhraseModel>>> list() {
        return success(service.list(requireDoctorUuid()));
    }

    @PostMapping(ApiUrl.DOCTOR_SMART_PHRASES)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<SmartPhraseModel>> create(
            @Valid @RequestBody SmartPhraseRequest request) {
        return success(service.create(requireDoctorUuid(), request));
    }

    @PutMapping(ApiUrl.DOCTOR_SMART_PHRASE_BY_UUID)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<SmartPhraseModel>> update(
            @PathVariable String phraseUuid, @Valid @RequestBody SmartPhraseRequest request) {
        return success(service.update(requireDoctorUuid(), phraseUuid, request));
    }

    @DeleteMapping(ApiUrl.DOCTOR_SMART_PHRASE_BY_UUID)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<Void>> delete(@PathVariable String phraseUuid) {
        service.delete(requireDoctorUuid(), phraseUuid);
        return success(null);
    }

    private String requireDoctorUuid() {
        String uuid = securityContextUtils.getCurrentUserUuid();
        if (uuid == null || uuid.isBlank()) {
            throw new com.kittyp.common.exception.CustomException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        return uuid;
    }

    private <T> ResponseEntity<SuccessResponse<T>> success(T data) {
        return responseBuilder.buildSuccessResponse(data, ResponseMessage.SUCCESS, HttpStatus.OK);
    }
}
