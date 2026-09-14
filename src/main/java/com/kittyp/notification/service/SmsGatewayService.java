package com.kittyp.notification.service;

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

	private static final String SEND_PATH = "/gateway/send-sms";

	private final RestClient restClient;
	private final String baseUrl;
	private final String apiKey;
	private final String deviceId;

	public SmsGatewayService(
			RestClient restClient,
			@Value("${textbee.base-url:https://api.textbee.dev/api/v1}") String baseUrl,
			@Value("${textbee.api-key:}") String apiKey,
			@Value("${textbee.device-id:}") String deviceId) {
		this.restClient = restClient;
		this.baseUrl = trimSlash(baseUrl);
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.deviceId = deviceId == null ? "" : deviceId.trim();
	}

	@Override
	public void sendOtp(String phoneNumber, String otpCode) {
		if (phoneNumber == null || phoneNumber.isBlank()) {
			throw new CustomException("Phone number is required", HttpStatus.BAD_REQUEST);
		}
		if (otpCode == null || otpCode.isBlank()) {
			throw new CustomException("OTP code is required", HttpStatus.BAD_REQUEST);
		}
		if (apiKey.isBlank() || deviceId.isBlank() || baseUrl.isBlank()) {
			throw new CustomException("SMS gateway is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}

		String phone = phoneNumber.trim();
		Map<String, Object> body = Map.of(
				"deviceId", deviceId,
				"recipients", List.of(phone),
				"message", "Your Kittyp verification code is " + otpCode.trim() + ". It expires in 10 minutes.");

		try {
			restClient.post()
					.uri(baseUrl + SEND_PATH)
					.header("x-api-key", apiKey)
					.body(body)
					.retrieve()
					.toBodilessEntity();
			log.info("SMS OTP sent to {}", maskPhone(phone));
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
}
