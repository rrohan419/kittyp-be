package com.kittyp.auth.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bastiaanjansen.otp.TOTPGenerator;
import com.kittyp.auth.dto.MasterTotpEnrollmentModel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MasterTotpService {

	private static final int DELAY_WINDOW = 3;
	private static final String ISSUER = "Kittyp";
	private static final String ACCOUNT = "master";
	private static final int DEFAULT_PERIOD_SECONDS = 30;
	private static final int DEFAULT_DIGITS = 6;

	private final TOTPGenerator totp;
	private final String enrollmentUri;

	public MasterTotpService(
			@Value("${kittyp.security.master-totp-secret:}") String secret) {
		TotpSetup setup = buildSetup(secret);
		this.totp = setup.totp;
		this.enrollmentUri = setup.enrollmentUri;
	}

	public boolean verifyMasterCode(String submittedCode) {
		if (totp == null || submittedCode == null || submittedCode.isBlank()) {
			return false;
		}
		return totp.verify(submittedCode.trim(), DELAY_WINDOW);
	}

	public MasterTotpEnrollmentModel enrollment() {
		int periodSeconds = totp == null ? DEFAULT_PERIOD_SECONDS : (int) totp.getPeriod().toSeconds();
		int digits = totp == null ? DEFAULT_DIGITS : totp.getPasswordLength();
		return new MasterTotpEnrollmentModel(
				totp != null,
				enrollmentUri,
				ISSUER,
				ACCOUNT,
				periodSeconds,
				digits);
	}

	Duration period() {
		return totp == null ? null : totp.getPeriod();
	}

	int passwordLength() {
		return totp == null ? 0 : totp.getPasswordLength();
	}

	String currentCode() {
		return totp == null ? null : totp.now();
	}

	private static TotpSetup buildSetup(String secret) {
		if (secret == null || secret.isBlank()) {
			return TotpSetup.disabled();
		}
		String trimmed = secret.trim().replace(" ", "");
		try {
			String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
			String uri = "otpauth://totp/" + ISSUER + ":" + ACCOUNT
					+ "?secret=" + encoded
					+ "&issuer=" + ISSUER
					+ "&period=" + DEFAULT_PERIOD_SECONDS
					+ "&digits=" + DEFAULT_DIGITS;
			return new TotpSetup(TOTPGenerator.fromURI(URI.create(uri)), uri);
		} catch (URISyntaxException | IllegalArgumentException e) {
			log.warn("Master TOTP secret is invalid; authenticator fallback disabled");
			return TotpSetup.disabled();
		}
	}

	private record TotpSetup(TOTPGenerator totp, String enrollmentUri) {
		static TotpSetup disabled() {
			return new TotpSetup(null, null);
		}
	}
}
