package com.kittyp.auth.service;

import org.springframework.http.HttpStatus;

import com.kittyp.common.dto.SignupClinicRequestDto;
import com.kittyp.common.dto.SignupDoctorRequestDto;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.VerificationCodeService;

/**
 * Shared doctor and clinic registration checks used by public signup and
 * authenticated role activation. Email OTP is required only for a new account.
 */
public final class SignupRequirements {

	private SignupRequirements() {
	}

	public static void requireDoctor(SignupDoctorRequestDto req, VerificationCodeService verification,
			boolean requireEmailOtp) {
		if (req.getPhoneNumber() == null || req.getPhoneNumber().isBlank()) {
			throw new CustomException("Phone number is required", HttpStatus.BAD_REQUEST);
		}
		if (req.getRegistrationNumber() == null || req.getRegistrationNumber().isBlank()) {
			throw new CustomException("Veterinary registration number is required", HttpStatus.BAD_REQUEST);
		}
		if (req.getDegreeCertificateUrl() == null || req.getDegreeCertificateUrl().isBlank()
				|| req.getRegistrationCertificateUrl() == null || req.getRegistrationCertificateUrl().isBlank()) {
			throw new CustomException("Degree and registration certificate uploads are required",
					HttpStatus.BAD_REQUEST);
		}
		if (requireEmailOtp
				&& !verification.isVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()))) {
			throw new CustomException("Email OTP verification required", HttpStatus.BAD_REQUEST);
		}
		if (!verification.isVerified(VerificationCodeService.phoneVerifiedKey(req.getPhoneNumber()))
				&& !verification.isVerified(
						VerificationCodeService.phoneVerifiedKey("+91" + req.getPhoneNumber().trim()))) {
			throw new CustomException("Phone OTP verification required", HttpStatus.BAD_REQUEST);
		}
	}

	public static void requireClinic(SignupClinicRequestDto req, VerificationCodeService verification,
			boolean requireEmailOtp) {
		if (req.getClinicName() == null || req.getClinicName().isBlank()) {
			throw new CustomException("Clinic name is required", HttpStatus.BAD_REQUEST);
		}
		if (requireEmailOtp
				&& !verification.isVerified(VerificationCodeService.emailVerifiedKey(req.getEmail()))) {
			throw new CustomException("Email OTP verification required", HttpStatus.BAD_REQUEST);
		}
	}
}
