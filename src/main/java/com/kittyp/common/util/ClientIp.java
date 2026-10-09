package com.kittyp.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * First address in {@code X-Forwarded-For}, or the socket address when that header is absent.
 */
public final class ClientIp {

	private ClientIp() {
	}

	public static String from(HttpServletRequest request) {
		if (request == null) {
			return "unknown";
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		String remote = request.getRemoteAddr();
		return remote == null || remote.isBlank() ? "unknown" : remote;
	}
}
