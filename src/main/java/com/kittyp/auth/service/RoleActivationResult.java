package com.kittyp.auth.service;

/**
 * Outcome of a committed role activation. Welcome email is sent by the caller
 * after this transaction returns.
 */
public record RoleActivationResult(String message, String firstName, WelcomeEmail welcome) {

	public enum WelcomeEmail {
		NONE,
		PARENT,
		DOCTOR,
		CLINIC
	}

	public static RoleActivationResult alreadyActive() {
		return new RoleActivationResult("This role is already on your account.", null, WelcomeEmail.NONE);
	}
}
