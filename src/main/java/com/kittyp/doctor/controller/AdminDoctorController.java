package com.kittyp.doctor.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.dto.DoctorChecklistUpdateRequest;
import com.kittyp.doctor.dto.DoctorStatusUpdateRequest;
import com.kittyp.doctor.dto.DoctorVerificationModel;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.doctor.enums.DoctorStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class AdminDoctorController {

    private final DoctorProfileDao doctorProfileDao;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.ADMIN_DOCTORS)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<List<DoctorVerificationModel>>> list(
            @RequestParam(required = false) DoctorStatus status) {
        List<DoctorProfile> profiles = status == null
                ? doctorProfileDao.findAllOrdered()
                : doctorProfileDao.findByStatus(status);
        return responseBuilder.buildSuccessResponse(profiles.stream().map(this::toModel).toList(),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.ADMIN_DOCTOR_BY_UUID)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> detail(@PathVariable String uuid) {
        return responseBuilder.buildSuccessResponse(toModel(doctorProfileDao.findByUuid(uuid)),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping(ApiUrl.ADMIN_DOCTOR_CHECKLIST)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> updateChecklist(
            @PathVariable String uuid, @RequestBody DoctorChecklistUpdateRequest request) {
        DoctorProfile profile = doctorProfileDao.findByUuid(uuid);
        if (request.getCheckMobileOtp() != null) profile.setCheckMobileOtp(request.getCheckMobileOtp());
        if (request.getCheckEmailOtp() != null) profile.setCheckEmailOtp(request.getCheckEmailOtp());
        if (request.getCheckGovernmentId() != null) profile.setCheckGovernmentId(request.getCheckGovernmentId());
        if (request.getCheckDegree() != null) profile.setCheckDegree(request.getCheckDegree());
        if (request.getCheckRegistrationCertificate() != null) {
            profile.setCheckRegistrationCertificate(request.getCheckRegistrationCertificate());
        }
        if (request.getCheckClinicAddress() != null) profile.setCheckClinicAddress(request.getCheckClinicAddress());
        if (request.getCheckRegistrationNumber() != null) {
            profile.setCheckRegistrationNumber(request.getCheckRegistrationNumber());
        }
        if (request.getCheckGoogleMapsMatch() != null) {
            profile.setCheckGoogleMapsMatch(request.getCheckGoogleMapsMatch());
        }
        if (request.getCheckClinicPhotos() != null) profile.setCheckClinicPhotos(request.getCheckClinicPhotos());

        if (profile.getStatus() == DoctorStatus.DOCUMENTS_SUBMITTED && anyChecked(profile)) {
            profile.setStatus(DoctorStatus.UNDER_REVIEW);
        }
        return responseBuilder.buildSuccessResponse(toModel(doctorProfileDao.save(profile)),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping(ApiUrl.ADMIN_DOCTOR_STATUS)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> updateStatus(
            @PathVariable String uuid, @Valid @RequestBody DoctorStatusUpdateRequest request) {
        DoctorProfile profile = doctorProfileDao.findByUuid(uuid);
        DoctorStatus next = request.getStatus();

        if (next == DoctorStatus.VERIFIED || next == DoctorStatus.PUBLISHED) {
            if (!allChecksPassed(profile)) {
                throw new CustomException(
                        "All verification checklist items must be confirmed before Verified/Published",
                        HttpStatus.BAD_REQUEST);
            }
        }

        profile.setStatus(next);
        if (request.getReviewNotes() != null) {
            profile.setReviewNotes(request.getReviewNotes());
        }
        if (next == DoctorStatus.VERIFIED || next == DoctorStatus.PUBLISHED || next == DoctorStatus.REJECTED) {
            profile.setReviewedAt(LocalDateTime.now());
        }

        return responseBuilder.buildSuccessResponse(toModel(doctorProfileDao.save(profile)),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private boolean anyChecked(DoctorProfile p) {
        return p.isCheckMobileOtp() || p.isCheckEmailOtp() || p.isCheckGovernmentId() || p.isCheckDegree()
                || p.isCheckRegistrationCertificate() || p.isCheckClinicAddress()
                || p.isCheckRegistrationNumber() || p.isCheckGoogleMapsMatch() || p.isCheckClinicPhotos();
    }

    private boolean allChecksPassed(DoctorProfile p) {
        return p.isCheckMobileOtp() && p.isCheckEmailOtp() && p.isCheckGovernmentId() && p.isCheckDegree()
                && p.isCheckRegistrationCertificate() && p.isCheckClinicAddress()
                && p.isCheckRegistrationNumber() && p.isCheckGoogleMapsMatch() && p.isCheckClinicPhotos();
    }

    private DoctorVerificationModel toModel(DoctorProfile p) {
        String clinicAddress = p.getClinic() != null ? p.getClinic().getAddress() : null;
        String specialization = p.getSpecialization() != null ? p.getSpecialization().name() : null;
        return new DoctorVerificationModel(
                p.getUuid(),
                p.getUser().getFirstName(),
                p.getUser().getLastName(),
                p.getUser().getEmail(),
                p.getPhoneNumber(),
                specialization,
                p.getRegistrationNumber(),
                p.getStatus(),
                p.getDegreeCertificateUrl(),
                p.getRegistrationCertificateUrl(),
                p.getGovernmentIdUrl(),
                p.getClinicPhotosUrls(),
                clinicAddress,
                p.isEmailOtpVerified(),
                p.isPhoneOtpVerified(),
                p.isCheckMobileOtp(),
                p.isCheckEmailOtp(),
                p.isCheckGovernmentId(),
                p.isCheckDegree(),
                p.isCheckRegistrationCertificate(),
                p.isCheckClinicAddress(),
                p.isCheckRegistrationNumber(),
                p.isCheckGoogleMapsMatch(),
                p.isCheckClinicPhotos(),
                p.getSubmittedAt(),
                p.getReviewedAt(),
                p.getReviewNotes());
    }
}
