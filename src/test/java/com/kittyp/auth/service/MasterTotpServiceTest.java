package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.kittyp.auth.dto.MasterTotpEnrollmentModel;

class MasterTotpServiceTest {

	private static final String SECRET = "JBSWY3DPEHPK3PXP";

	@Test
	void currentCode_verifiesWithinWindow() {
		MasterTotpService service = new MasterTotpService(SECRET);
		assertEquals(Duration.ofSeconds(30), service.period());
		assertEquals(6, service.passwordLength());
		String code = service.currentCode();
		assertNotNull(code);
		assertEquals(6, code.length());
		assertTrue(service.verifyMasterCode(code));
	}

	@Test
	void garbageCode_fails() {
		MasterTotpService service = new MasterTotpService(SECRET);
		assertFalse(service.verifyMasterCode("000000"));
		assertFalse(service.verifyMasterCode(""));
		assertFalse(service.verifyMasterCode(null));
	}

	@Test
	void blankSecret_alwaysFalse() {
		MasterTotpService service = new MasterTotpService("");
		assertFalse(service.verifyMasterCode("123456"));
		MasterTotpEnrollmentModel enrollment = service.enrollment();
		assertFalse(enrollment.enabled());
		assertNull(enrollment.otpauthUri());
	}

	@Test
	void enrollment_includesOtpauthUri() {
		MasterTotpService service = new MasterTotpService(SECRET);
		MasterTotpEnrollmentModel enrollment = service.enrollment();
		assertTrue(enrollment.enabled());
		assertEquals("Kittyp", enrollment.issuer());
		assertEquals("master", enrollment.account());
		assertEquals(30, enrollment.periodSeconds());
		assertEquals(6, enrollment.digits());
		assertNotNull(enrollment.otpauthUri());
		assertTrue(enrollment.otpauthUri().startsWith("otpauth://totp/Kittyp:master?secret="));
		assertTrue(enrollment.otpauthUri().contains(SECRET));
		assertTrue(enrollment.otpauthUri().contains("issuer=Kittyp"));
		assertTrue(enrollment.otpauthUri().contains("period=30"));
		assertTrue(enrollment.otpauthUri().contains("digits=6"));
	}
}
