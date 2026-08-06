package com.kittyp.doctor.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

import com.kittyp.clinic.repository.ClinicDoctorRepository;
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
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.ADMIN_DOCTORS)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<List<DoctorVerificationModel>>> list(
            @RequestParam(required = false) DoctorStatus status) {
        Set<Long> clinicLinkedIds = clinicDoctorRepository.findActiveAffiliatedDoctorIds();
        List<DoctorProfile> profiles = status == null
                ? doctorProfileDao.findAllOrdered()
                : doctorProfileDao.findByStatus(status);
        return responseBuilder.buildSuccessResponse(
                profiles.stream().map(p -> toModel(p, clinicLinkedIds)).toList(),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.ADMIN_DOCTOR_BY_UUID)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> detail(@PathVariable String uuid) {
        DoctorProfile profile = doctorProfileDao.findByUuid(uuid);
        Set<Long> clinicLinkedIds = clinicDoctorRepository.findActiveAffiliatedDoctorIds();
        return responseBuilder.buildSuccessResponse(toModel(profile, clinicLinkedIds),
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
        Set<Long> clinicLinkedIds = clinicDoctorRepository.findActiveAffiliatedDoctorIds();
        return responseBuilder.buildSuccessResponse(toModel(doctorProfileDao.save(profile), clinicLinkedIds),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @PatchMapping(ApiUrl.ADMIN_DOCTOR_STATUS)
    @PreAuthorize(KeyConstant.IS_ROLE_ADMIN_OR_MODERATOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> updateStatus(
            @PathVariable String uuid, @Valid @RequestBody DoctorStatusUpdateRequest request) {
        DoctorProfile profile = doctorProfileDao.findByUuid(uuid);
        DoctorStatus next = request.getStatus();

        if (next == DoctorStatus.VERIFIED || next == DoctorStatus.PUBLISHED) {
            if (!allApplicableChecksPassed(profile)) {
                throw new CustomException(
                        "All applicable verification checklist items must be confirmed before Verified/Published",
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

        Set<Long> clinicLinkedIds = clinicDoctorRepository.findActiveAffiliatedDoctorIds();
        return responseBuilder.buildSuccessResponse(toModel(doctorProfileDao.save(profile), clinicLinkedIds),
                ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    private boolean anyChecked(DoctorProfile p) {
        return p.isCheckMobileOtp() || p.isCheckEmailOtp() || p.isCheckGovernmentId() || p.isCheckDegree()
                || p.isCheckRegistrationCertificate() || p.isCheckClinicAddress()
                || p.isCheckRegistrationNumber() || p.isCheckGoogleMapsMatch() || p.isCheckClinicPhotos();
    }

    /**
     * Core checks always required. Clinic / gov-id / photos only when the doctor provided that data
     * or is associated with a clinic.
     */
    private boolean allApplicableChecksPassed(DoctorProfile p) {
        if (!p.isCheckMobileOtp() || !p.isCheckEmailOtp()) {
            return false;
        }
        if (!p.isCheckDegree() || !p.isCheckRegistrationCertificate() || !p.isCheckRegistrationNumber()) {
            return false;
        }
        if (requiresGovernmentIdCheck(p) && !p.isCheckGovernmentId()) {
            return false;
        }
        if (requiresClinicChecks(p)) {
            if (!p.isCheckClinicAddress() || !p.isCheckGoogleMapsMatch()) {
                return false;
            }
            if (requiresClinicPhotosCheck(p) && !p.isCheckClinicPhotos()) {
                return false;
            }
        }
        return true;
    }

    private boolean requiresClinicChecks(DoctorProfile p) {
        return p.getClinic() != null
                || clinicDoctorRepository.existsByDoctor_IdAndIsActiveTrue(p.getId());
    }

    private boolean requiresGovernmentIdCheck(DoctorProfile p) {
        return hasText(p.getGovernmentIdUrl());
    }

    private boolean requiresClinicPhotosCheck(DoctorProfile p) {
        return requiresClinicChecks(p) && hasText(p.getClinicPhotosUrls());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private DoctorVerificationModel toModel(DoctorProfile p, Set<Long> clinicLinkedIds) {
        boolean hasClinic = p.getClinic() != null;
        boolean clinicPriority = hasClinic || clinicLinkedIds.contains(p.getId());
        String clinicAddress = hasClinic ? p.getClinic().getAddress() : null;
        String clinicName = hasClinic ? p.getClinic().getName() : null;
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
                clinicName,
                hasClinic || clinicPriority,
                clinicPriority,
                requiresGovernmentIdCheck(p),
                requiresClinicChecks(p),
                requiresClinicPhotosCheck(p),
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
