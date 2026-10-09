package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kittyp.auth.service.RolePasswordMatch.Decision;
import com.kittyp.auth.service.RolePasswordMatch.Kind;
import com.kittyp.user.enums.ERole;

class RolePasswordMatchTest {

	@Test
	void uniqueRoleWinsEvenWhenAccountPasswordMatches() {
		Decision decision = RolePasswordMatch.decide(true, List.of(ERole.ROLE_DOCTOR));

		assertEquals(Kind.UNIQUE, decision.kind());
		assertEquals(ERole.ROLE_DOCTOR, decision.loginRole());
	}

	@Test
	void uniqueRoleWinsWhenAccountPasswordDoesNotMatch() {
		Decision decision = RolePasswordMatch.decide(false, List.of(ERole.ROLE_USER));

		assertEquals(Kind.UNIQUE, decision.kind());
		assertEquals(ERole.ROLE_USER, decision.loginRole());
	}

	@Test
	void accountPasswordWithoutRoleCredentialKeepsPreferredWorkspace() {
		Decision decision = RolePasswordMatch.decide(true, List.of());

		assertEquals(Kind.ACCOUNT, decision.kind());
		assertNull(decision.loginRole());
	}

	@Test
	void samePasswordOnSeveralRolesKeepsPreferredWorkspace() {
		Decision decision = RolePasswordMatch.decide(true, List.of(ERole.ROLE_USER, ERole.ROLE_DOCTOR));

		assertEquals(Kind.AMBIGUOUS, decision.kind());
		assertNull(decision.loginRole());
	}

	@Test
	void severalRoleHashesWithoutAccountPasswordStillHaveNoLoginRole() {
		Decision decision = RolePasswordMatch.decide(false, List.of(ERole.ROLE_CLINIC_ADMIN, ERole.ROLE_DOCTOR));

		assertEquals(Kind.AMBIGUOUS, decision.kind());
		assertNull(decision.loginRole());
	}

	@Test
	void unknownPasswordIsRejected() {
		Decision decision = RolePasswordMatch.decide(false, List.of());

		assertEquals(Kind.REJECT, decision.kind());
		assertNull(decision.loginRole());
	}
}
