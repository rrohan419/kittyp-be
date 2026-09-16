package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kittyp.auth.dto.SignupOtpVerifyRequest;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.util.VerificationCodeService;

class AuthServiceImplOtpTest {

	private static final String SECRET = "JBSWY3DPEHPK3PXP";
	private static final String PHONE = "+919876543210";

	private VerificationCodeService verificationCodeService;
	private MasterTotpService masterTotpService;
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		verificationCodeService = new VerificationCodeService();
		masterTotpService = new MasterTotpService(SECRET);
		authService = new AuthServiceImpl(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				verificationCodeService,
				null,
				masterTotpService,
				null,
				null,
				null);
	}

	@Test
	void verifySignupOtp_phone_acceptsSmsCode() {
		String sms = verificationCodeService.generateCode(VerificationCodeService.phoneOtpKey(PHONE));
		Map<String, Boolean> result = authService.verifySignupOtp(phoneRequest(sms));
		assertTrue(result.get("verified"));
		assertTrue(verificationCodeService.isVerified(VerificationCodeService.phoneVerifiedKey(PHONE)));
	}

	@Test
	void verifySignupOtp_phone_fallsBackToMasterTotp() {
		verificationCodeService.generateCode(VerificationCodeService.phoneOtpKey(PHONE));
		Map<String, Boolean> result = authService.verifySignupOtp(phoneRequest(masterTotpService.currentCode()));
		assertTrue(result.get("verified"));
		assertTrue(verificationCodeService.isVerified(VerificationCodeService.phoneVerifiedKey(PHONE)));
	}

	@Test
	void verifySignupOtp_phone_bothMiss_throws() {
		verificationCodeService.generateCode(VerificationCodeService.phoneOtpKey(PHONE));
		CustomException ex = assertThrows(CustomException.class, () -> authService.verifySignupOtp(phoneRequest("000000")));
		assertEquals("Invalid or expired OTP", ex.getMessage());
	}

	@Test
	void verifySignupOtp_phone_totpClearsAttemptLimit() {
		String otpKey = VerificationCodeService.phoneOtpKey(PHONE);
		verificationCodeService.generateCode(otpKey);
		for (int i = 0; i < 5; i++) {
			assertThrows(CustomException.class, () -> authService.verifySignupOtp(phoneRequest("000000")));
		}
		Map<String, Boolean> result = authService.verifySignupOtp(phoneRequest(masterTotpService.currentCode()));
		assertTrue(result.get("verified"));
		assertEquals(HttpStatus.BAD_REQUEST,
				assertThrows(CustomException.class, () -> authService.verifySignupOtp(phoneRequest("000000")))
						.getHttpStatus());
	}

	private static SignupOtpVerifyRequest phoneRequest(String code) {
		SignupOtpVerifyRequest request = new SignupOtpVerifyRequest();
		request.setChannel("PHONE");
		request.setPhone(PHONE);
		request.setCode(code);
		return request;
	}
}
