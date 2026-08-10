package com.kittyp.clinic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.dto.DiscoverDtos.DiscoverClinicCard;
import com.kittyp.clinic.dto.DiscoverDtos.DiscoverDoctorCard;
import com.kittyp.clinic.service.DiscoverService;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DiscoverController {

    private final DiscoverService discoverService;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.DISCOVER_CLINICS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<DiscoverClinicCard>>> discoverClinics(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String q) {
        return responseBuilder.buildSuccessResponse(
                discoverService.discoverClinics(lat, lng, radiusKm, city, q),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.DISCOVER_DOCTORS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<DiscoverDoctorCard>>> discoverPersonalDoctors(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String q) {
        return responseBuilder.buildSuccessResponse(
                discoverService.discoverPersonalDoctors(lat, lng, radiusKm, city, q),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @GetMapping(ApiUrl.DISCOVER_CLINIC_DOCTORS)
    @PreAuthorize(KeyConstant.IS_AUTHENTICATED)
    public ResponseEntity<SuccessResponse<List<DiscoverDoctorCard>>> discoverDoctors(
            @PathVariable String clinicUuid) {
        return responseBuilder.buildSuccessResponse(
                discoverService.discoverClinicDoctors(clinicUuid),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }
}
