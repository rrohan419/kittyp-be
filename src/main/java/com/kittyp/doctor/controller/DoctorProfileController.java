package com.kittyp.doctor.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.clinic.repository.ClinicDoctorRepository;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.dto.DoctorVerificationModel;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.notification.service.WhatsAppCredentialsVerifier;
import com.kittyp.notification.service.WhatsAppSettingsSupport;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileDao doctorProfileDao;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final UserDao userDao;
    private final ApiResponse<?> responseBuilder;
    private final WhatsAppCredentialsVerifier whatsAppCredentialsVerifier;

    @GetMapping(ApiUrl.DOCTOR_ME)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> myProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userDao.userByEmail(email);
        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        Set<Long> clinicLinkedIds = clinicDoctorRepository.findActiveAffiliatedDoctorIds();
        boolean hasClinic = profile.getClinic() != null;
        boolean clinicPriority = hasClinic || clinicLinkedIds.contains(profile.getId());
        boolean requiresGovId = hasText(profile.getGovernmentIdUrl());
        String clinicAddress = hasClinic ? profile.getClinic().getAddress() : null;
        String clinicName = hasClinic ? profile.getClinic().getName() : null;
        String specialization = profile.getSpecialization() != null ? profile.getSpecialization().name() : null;

        DoctorVerificationModel model = new DoctorVerificationModel(
                profile.getUuid(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                profile.getPhoneNumber(),
                specialization,
                profile.getRegistrationNumber(),
                profile.getStatus(),
                profile.getDegreeCertificateUrl(),
                profile.getRegistrationCertificateUrl(),
                profile.getGovernmentIdUrl(),
                profile.getClinicPhotosUrls(),
                clinicAddress,
                clinicName,
                hasClinic || clinicPriority,
                clinicPriority,
                requiresGovId,
                false,
                false,
                profile.isEmailOtpVerified(),
                profile.isPhoneOtpVerified(),
                profile.isCheckMobileOtp(),
                profile.isCheckEmailOtp(),
                profile.isCheckGovernmentId(),
                profile.isCheckDegree(),
                profile.isCheckRegistrationCertificate(),
                profile.isCheckClinicAddress(),
                profile.isCheckRegistrationNumber(),
                profile.isCheckGoogleMapsMatch(),
                profile.isCheckClinicPhotos(),
                profile.getSubmittedAt(),
                profile.getReviewedAt(),
                profile.getReviewNotes());
        return responseBuilder.buildSuccessResponse(model, ResponseMessage.SUCCESS, HttpStatus.OK);
    }

    @GetMapping(ApiUrl.DOCTOR_WHATSAPP_SETTINGS)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<Map<String, Object>>> getWhatsAppSettings() {
        DoctorProfile profile = requireMyProfile();
        return responseBuilder.buildSuccessResponse(
                WhatsAppSettingsSupport.publicView(
                        profile.getWhatsappPhoneNumberId(),
                        profile.getWhatsappBusinessAccountId(),
                        profile.getWhatsappToken()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    @PutMapping(ApiUrl.DOCTOR_WHATSAPP_SETTINGS)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<Map<String, Object>>> updateWhatsAppSettings(
            @Valid @RequestBody WhatsAppSettingsRequest request) {
        DoctorProfile profile = requireMyProfile();
        String phoneNumberId = request.getPhoneNumberId().trim();
        String businessAccountId = request.getBusinessAccountId().trim();
        String tokenToStore;
        if (StringUtils.hasText(request.getToken())) {
            tokenToStore = request.getToken().trim();
        } else if (StringUtils.hasText(profile.getWhatsappToken())) {
            tokenToStore = profile.getWhatsappToken();
        } else {
            throw new CustomException("token is required for first-time WhatsApp setup", HttpStatus.BAD_REQUEST);
        }
        whatsAppCredentialsVerifier.verifyOrThrow(tokenToStore, phoneNumberId, businessAccountId);
        profile.setWhatsappPhoneNumberId(phoneNumberId);
        profile.setWhatsappBusinessAccountId(businessAccountId);
        profile.setWhatsappToken(tokenToStore);
        doctorProfileDao.save(profile);
        return responseBuilder.buildSuccessResponse(
                WhatsAppSettingsSupport.publicView(
                        profile.getWhatsappPhoneNumberId(),
                        profile.getWhatsappBusinessAccountId(),
                        profile.getWhatsappToken()),
                ResponseMessage.SUCCESS,
                HttpStatus.OK);
    }

    private DoctorProfile requireMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userDao.userByEmail(email);
        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        return profile;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Data
    public static class WhatsAppSettingsRequest {
        @NotBlank
        @jakarta.validation.constraints.Size(max = 64)
        private String phoneNumberId;
        @NotBlank
        @jakarta.validation.constraints.Size(max = 64)
        private String businessAccountId;
        /** Optional on update if already set — omit to keep existing token. */
        @jakarta.validation.constraints.Size(max = 2048)
        private String token;
    }
}
