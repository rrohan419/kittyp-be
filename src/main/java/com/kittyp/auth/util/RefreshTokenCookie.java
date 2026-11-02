package com.kittyp.auth.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.kittyp.common.constants.ApiUrl;

import jakarta.servlet.http.HttpServletResponse;

/** Writes the HttpOnly cookie that carries the refresh token. */
@Component
public class RefreshTokenCookie {

	public static final String NAME = "kittyp_rt";

	// SameSite=None is needed when the SPA and API are on different sites; it requires Secure.
	@Value("${jwt.refresh-cookie.secure:true}")
	private boolean secure;

	@Value("${jwt.refresh-cookie.same-site:None}")
	private String sameSite;

	@Value("${jwt.refresh-expiration}")
	private long refreshExpirationMs;

	public void write(HttpServletResponse response, String token) {
		response.addHeader(HttpHeaders.SET_COOKIE, build(token, refreshExpirationMs / 1000).toString());
	}

	public void clear(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
	}

	private ResponseCookie build(String value, long maxAgeSeconds) {
		return ResponseCookie.from(NAME, value)
				.httpOnly(true)
				.secure(secure)
				.sameSite(sameSite)
				.path("/" + ApiUrl.BASE_URL + ApiUrl.AUTH_BASE_URL)
				.maxAge(maxAgeSeconds)
				.build();
	}
}
