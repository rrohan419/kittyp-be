package com.kittyp.notification.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
public class SmsGatewayService implements SmsService {

	private static final String SEND_PATH = "/message";

	private final RestClient restClient;
	private final String baseUrl;
	private final String username;
	private final String password;

	public SmsGatewayService(
			RestClient restClient,
			@Value("${sms.base-url:}") String baseUrl,
			@Value("${sms.username:}") String username,
			@Value("${sms.password:}") String password) {
		this.restClient = restClient;
		this.baseUrl = trimSlash(baseUrl);
		this.username = username == null ? "" : username.trim();
		this.password = password == null ? "" : password.trim();
	}

	@Override
	public void sendOtp(String phoneNumber, String otpCode) {
		if (phoneNumber == null || phoneNumber.isBlank()) {
			throw new CustomException("Phone number is required", HttpStatus.BAD_REQUEST);
		}
		if (otpCode == null || otpCode.isBlank()) {
			throw new CustomException("OTP code is required", HttpStatus.BAD_REQUEST);
		}
		if (baseUrl.isBlank() || username.isBlank() || password.isBlank()) {
			throw new CustomException("SMS gateway is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}

		String phone = toE164(phoneNumber);
		Map<String, Object> body = Map.of(
				"textMessage", Map.of("text",
						"Kittyp code " + otpCode.trim() + ". Valid 10 min."),
				"phoneNumbers", List.of(phone));

		try {
			String response = restClient.post()
					.uri(baseUrl + SEND_PATH)
					.header("Authorization", basicAuth(username, password))
					.header("Content-Type", "application/json")
					.body(body)
					.retrieve()
					.body(String.class);
			log.info("SMS OTP queued for {} smsgate={}", maskPhone(phone), truncate(response));
		} catch (RestClientResponseException e) {
			String snippet = e.getResponseBodyAsString();
			if (snippet != null && snippet.length() > 200) {
				snippet = snippet.substring(0, 200);
			}
			log.warn("SMS gateway HTTP {} for {}: {}", e.getStatusCode().value(), maskPhone(phone), snippet);
			throw new CustomException("Failed to send SMS", HttpStatus.BAD_GATEWAY, e);
		} catch (ResourceAccessException e) {
			log.warn("SMS gateway unreachable for {}: {}", maskPhone(phone), e.getMessage());
			throw new CustomException("Failed to send SMS", HttpStatus.SERVICE_UNAVAILABLE, e);
		}
	}

	private static String basicAuth(String user, String pass) {
		String token = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}

	private static String trimSlash(String url) {
		if (url == null || url.isBlank()) {
			return "";
		}
		String trimmed = url.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static String maskPhone(String phone) {
		String digits = phone.replaceAll("\\D", "");
		if (digits.length() <= 4) {
			return "****";
		}
		return "****" + digits.substring(digits.length() - 4);
	}

	/** SMS Gate expects E.164. Indian numbers use +91 plus last 10 digits. */
	static String toE164(String phoneNumber) {
		String digits = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
		if (digits.length() >= 10) {
			return "+91" + digits.substring(digits.length() - 10);
		}
		return digits.isEmpty() ? "" : "+" + digits;
	}

	private static String truncate(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		String trimmed = body.trim();
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}
}
