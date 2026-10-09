package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.kittyp.auth.service.RolePasswordMatch.Decision;
import com.kittyp.auth.service.RolePasswordMatch.Kind;
import com.kittyp.user.enums.ERole;

class AuthServiceImplLoginFilterTest {

	@Test
	void staleCredentialForRemovedRoleIsIgnored() {
		Decision decision = decide(false, List.of(ERole.ROLE_USER), List.of(ERole.ROLE_DOCTOR));

		assertEquals(Kind.REJECT, decision.kind());
		assertNull(decision.loginRole());
	}

	@Test
	void validRoleStillWinsWhenStaleCredentialAlsoMatches() {
		Decision decision = decide(true, List.of(ERole.ROLE_USER), List.of(ERole.ROLE_USER, ERole.ROLE_DOCTOR));

		assertEquals(Kind.UNIQUE, decision.kind());
		assertEquals(ERole.ROLE_USER, decision.loginRole());
	}

	@Test
	void twoAssignedRoleCredentialsProduceNoLoginRole() {
		Decision decision = decide(true, List.of(ERole.ROLE_USER, ERole.ROLE_CLINIC_ADMIN),
				List.of(ERole.ROLE_USER, ERole.ROLE_CLINIC_ADMIN));

		assertEquals(Kind.AMBIGUOUS, decision.kind());
		assertNull(decision.loginRole());
	}

	@Test
	void accountPasswordOnlyKeepsPreferredWorkspace() {
		Decision decision = decide(true, List.of(ERole.ROLE_USER, ERole.ROLE_CLINIC_ADMIN), List.of());

		assertEquals(Kind.ACCOUNT, decision.kind());
		assertNull(decision.loginRole());
	}

	private static Decision decide(boolean accountMatch, List<ERole> assigned, List<ERole> credentialHits) {
		List<SimpleGrantedAuthority> authorities = assigned.stream()
				.map(role -> new SimpleGrantedAuthority(role.name()))
				.toList();
		return RolePasswordMatch.decide(accountMatch, AuthServiceImpl.assignedRoleHits(authorities, credentialHits));
	}
}
