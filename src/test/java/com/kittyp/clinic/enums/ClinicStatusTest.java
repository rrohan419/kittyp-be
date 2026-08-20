package com.kittyp.clinic.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ClinicStatusTest {

	@Test
	void verifiedIsActivated() {
		assertTrue(ClinicStatus.VERIFIED.isActivated());
	}

	@ParameterizedTest
	@EnumSource(value = ClinicStatus.class, names = { "PENDING", "REJECTED", "SHUTDOWN" })
	void unverifiedNotActivated(ClinicStatus status) {
		assertFalse(status.isActivated());
	}

	@Test
	void activationMessageStable() {
		assertEquals(
				"This clinic must be verified by admin before appointments, bookings, or adding doctors.",
				ClinicStatus.NOT_ACTIVATED_MESSAGE);
	}
}
