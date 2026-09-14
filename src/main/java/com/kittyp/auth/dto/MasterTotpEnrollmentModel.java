package com.kittyp.auth.dto;

public record MasterTotpEnrollmentModel(
		boolean enabled,
		String otpauthUri,
		String issuer,
		String account,
		int periodSeconds,
		int digits) {
}
