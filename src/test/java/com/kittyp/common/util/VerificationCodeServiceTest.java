package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kittyp.common.exception.CustomException;

class VerificationCodeServiceTest {

	private VerificationCodeService service;

	@BeforeEach
	void setUp() {
		service = new VerificationCodeService();
	}

	@Test
	void verifyCode_wrongOtp_returnsFalse() {
		String key = VerificationCodeService.emailOtpKey("doc@example.com");
		String code = service.generateCode(key);
		assertFalse(service.verifyCode(key, "000000", true));
		assertTrue(service.verifyCode(key, code, true));
	}

	@Test
	void verifyCode_tooManyAttempts_throws429() {
		String key = VerificationCodeService.emailOtpKey("lock@example.com");
		service.generateCode(key);
		for (int i = 0; i < 5; i++) {
			assertFalse(service.verifyCode(key, "111111", true));
		}
		CustomException ex = assertThrows(CustomException.class,
				() -> service.verifyCode(key, "111111", true));
		assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
		assertEquals("Too many invalid OTP attempts. Please request a new code.", ex.getMessage());
	}

	@Test
	void generateCode_sendRateLimit_throws429() {
		String key = VerificationCodeService.emailOtpKey("flood@example.com");
		for (int i = 0; i < 5; i++) {
			service.generateCode(key);
		}
		CustomException ex = assertThrows(CustomException.class, () -> service.generateCode(key));
		assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getHttpStatus());
		assertEquals("Too many OTP requests. Please try again later.", ex.getMessage());
	}
}
