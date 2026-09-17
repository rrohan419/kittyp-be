package com.kittyp.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VerificationCodeServicePhoneNormalizeTest {

	@Test
	void normalizePhone_unifiesLocalAndE164() {
		assertEquals("+919876543210", VerificationCodeService.normalizePhone("9876543210"));
		assertEquals("+919876543210", VerificationCodeService.normalizePhone("+91 98765 43210"));
		assertEquals("+919876543210", VerificationCodeService.normalizePhone("+919876543210"));
	}

	@Test
	void phoneOtpKey_sameForLocalAndE164() {
		assertEquals(
				VerificationCodeService.phoneOtpKey("9876543210"),
				VerificationCodeService.phoneOtpKey("+919876543210"));
	}

	@Test
	void codeMatches_doesNotConsumeCode() {
		VerificationCodeService svc = new VerificationCodeService();
		String key = VerificationCodeService.emailOtpKey("doc@example.com");
		String code = svc.generateCode(key);
		assertTrue(svc.codeMatches(key, code));
		assertTrue(svc.codeMatches(key, code));
		assertTrue(svc.verifyCode(key, code, true));
		assertFalse(svc.codeMatches(key, code));
	}
}
