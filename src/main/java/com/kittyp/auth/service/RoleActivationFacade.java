package com.kittyp.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kittyp.auth.dto.ActivateRoleRequest;
import com.kittyp.common.model.MessageResponse;
import com.kittyp.email.service.ZeptoMailService;

/**
 * Runs role activation, then sends the welcome email only after the
 * transactional method has returned. An email failure does not undo the role.
 */
@Service
public class RoleActivationFacade {

	private static final Logger log = LoggerFactory.getLogger(RoleActivationFacade.class);

	private final RoleActivationService roleActivationService;
	private final ZeptoMailService zeptoMailService;

	public RoleActivationFacade(RoleActivationService roleActivationService, ZeptoMailService zeptoMailService) {
		this.roleActivationService = roleActivationService;
		this.zeptoMailService = zeptoMailService;
	}

	public MessageResponse activate(String sessionEmail, ActivateRoleRequest request) {
		RoleActivationResult result = roleActivationService.activate(sessionEmail, request);
		sendWelcome(sessionEmail, result);
		return new MessageResponse(result.message());
	}

	private void sendWelcome(String sessionEmail, RoleActivationResult result) {
		try {
			switch (result.welcome()) {
				case PARENT -> zeptoMailService.sendWelcomeEmailforParent(result.firstName(), sessionEmail);
				case DOCTOR -> zeptoMailService.sendWelcomeEmailforDoctor(sessionEmail);
				case CLINIC -> zeptoMailService.sendWelcomeEmailforClinicAdmin(sessionEmail);
				case NONE -> {
					// Idempotent retry: the role was already on the account.
				}
			}
		} catch (RuntimeException ex) {
			log.warn("Welcome email failed after role activation for {}: {}", sessionEmail, ex.getMessage());
		}
	}
}
