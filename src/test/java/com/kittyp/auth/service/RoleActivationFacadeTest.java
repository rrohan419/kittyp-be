package com.kittyp.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kittyp.auth.dto.ActivateRoleRequest;
import com.kittyp.common.enums.SignupRole;
import com.kittyp.email.service.ZeptoMailService;

class RoleActivationFacadeTest {

	private RoleActivationService roleActivationService;
	private ZeptoMailService zeptoMailService;
	private RoleActivationFacade facade;

	@BeforeEach
	void setUp() {
		roleActivationService = org.mockito.Mockito.mock(RoleActivationService.class);
		zeptoMailService = org.mockito.Mockito.mock(ZeptoMailService.class);
		facade = new RoleActivationFacade(roleActivationService, zeptoMailService);
	}

	@Test
	void failedActivation_doesNotSendWelcomeEmail() {
		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.USER);
		when(roleActivationService.activate("ada@example.com", request))
				.thenThrow(new IllegalStateException("link failed"));

		assertThrows(IllegalStateException.class, () -> facade.activate("ada@example.com", request));

		verify(zeptoMailService, never()).sendWelcomeEmailforParent(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any());
		verify(zeptoMailService, never()).sendWelcomeEmailforDoctor(org.mockito.ArgumentMatchers.any());
		verify(zeptoMailService, never()).sendWelcomeEmailforClinicAdmin(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void successfulParentActivation_sendsWelcomeEmailAfterCommitBoundary() {
		ActivateRoleRequest request = new ActivateRoleRequest();
		request.setRole(SignupRole.USER);
		when(roleActivationService.activate("ada@example.com", request)).thenReturn(
				new RoleActivationResult(RoleActivationService.PARENT_ADDED, "Ada",
						RoleActivationResult.WelcomeEmail.PARENT));

		assertEquals(RoleActivationService.PARENT_ADDED, facade.activate("ada@example.com", request).getMessage());
		verify(zeptoMailService).sendWelcomeEmailforParent("Ada", "ada@example.com");
	}
}
