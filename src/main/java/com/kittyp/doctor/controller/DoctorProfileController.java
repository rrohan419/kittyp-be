package com.kittyp.doctor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kittyp.common.constants.ApiUrl;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.constants.ResponseMessage;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.dto.SuccessResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.dto.DoctorVerificationModel;
import com.kittyp.doctor.entity.DoctorProfile;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileDao doctorProfileDao;
    private final UserDao userDao;
    private final ApiResponse<?> responseBuilder;

    @GetMapping(ApiUrl.DOCTOR_ME)
    @PreAuthorize(KeyConstant.IS_ROLE_DOCTOR)
    public ResponseEntity<SuccessResponse<DoctorVerificationModel>> myProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userDao.userByEmail(email);
        DoctorProfile profile = doctorProfileDao.findByUserId(user.getId());
        if (profile == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        String clinicAddress = profile.getClinic() != null ? profile.getClinic().getAddress() : null;
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
}
