package com.kittyp.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.kittyp.user.enums.ERole;

class SignupRoleTest {

	@Test
	void toERole_mapsAllowlistedValuesOnly() {
		assertEquals(ERole.ROLE_USER, SignupRole.USER.toERole());
		assertEquals(ERole.ROLE_DOCTOR, SignupRole.DOCTOR.toERole());
		assertEquals(ERole.ROLE_CLINIC_ADMIN, SignupRole.CLINIC.toERole());
	}

	@Test
	void valueOf_rejectsElevatedRoles() {
		assertThrows(IllegalArgumentException.class, () -> SignupRole.valueOf("ROLE_ADMIN"));
		assertThrows(IllegalArgumentException.class, () -> SignupRole.valueOf("ADMIN"));
		assertThrows(IllegalArgumentException.class, () -> SignupRole.valueOf("ROLE_MODERATOR"));
		assertThrows(IllegalArgumentException.class, () -> SignupRole.valueOf("ROLE_CLINIC_STAFF"));
	}
}
