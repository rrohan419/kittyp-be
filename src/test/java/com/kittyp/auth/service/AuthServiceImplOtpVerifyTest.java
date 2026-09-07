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

class AuthServiceImplOtpVerifyTest {

	private VerificationCodeService verificationCodeService;
	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		verificationCodeService = new VerificationCodeService();
		authService = new AuthServiceImpl(
				null, null, null, null, null, null, null, null, null, null, null,
				verificationCodeService, null, null, null);
	}

	@Test
	void verifySignupOtp_invalid_returns400WithMessage() {
		SignupOtpVerifyRequest request = new SignupOtpVerifyRequest();
		request.setChannel("EMAIL");
		request.setEmail("doc@example.com");
		request.setCode("123456");

		CustomException ex = assertThrows(CustomException.class, () -> authService.verifySignupOtp(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertEquals("Invalid or expired OTP", ex.getMessage());
	}

	@Test
	void verifySignupOtp_valid_marksVerified() {
		String email = "doc-ok@example.com";
		String code = verificationCodeService.generateCode(VerificationCodeService.emailOtpKey(email));
		SignupOtpVerifyRequest request = new SignupOtpVerifyRequest();
		request.setChannel("EMAIL");
		request.setEmail(email);
		request.setCode(code);

		Map<String, Boolean> result = authService.verifySignupOtp(request);
		assertTrue(result.get("verified"));
		assertTrue(verificationCodeService.isVerified(VerificationCodeService.emailVerifiedKey(email)));
	}
}
