package com.kittyp.doctor.dto;

import java.time.LocalDateTime;

import com.kittyp.doctor.enums.DoctorStatus;

public record DoctorVerificationModel(
        String uuid,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String specialization,
        String registrationNumber,
        DoctorStatus status,
        String degreeCertificateUrl,
        String registrationCertificateUrl,
        String governmentIdUrl,
        String clinicPhotosUrls,
        String clinicAddress,
        String clinicName,
        boolean hasClinic,
        boolean clinicPriority,
        boolean requiresGovernmentIdCheck,
        boolean requiresClinicChecks,
        boolean requiresClinicPhotosCheck,
        boolean emailOtpVerified,
        boolean phoneOtpVerified,
        boolean checkMobileOtp,
        boolean checkEmailOtp,
        boolean checkGovernmentId,
        boolean checkDegree,
        boolean checkRegistrationCertificate,
        boolean checkClinicAddress,
        boolean checkRegistrationNumber,
        boolean checkGoogleMapsMatch,
        boolean checkClinicPhotos,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String reviewNotes) {
}
