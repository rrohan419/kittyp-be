package com.kittyp.notification.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.logging.PiiMasker;
import com.kittyp.email.service.ZeptoMailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
public class SmsGatewayService implements SmsService {

	private static final String SEND_PATH = "/message";

	private final RestClient restClient;
	private final ZeptoMailService zeptoMailService;
	private final String baseUrl;
	private final String username;
	private final String password;

	@Autowired
	public SmsGatewayService(
			@Qualifier("smsRestClient") RestClient restClient,
			ZeptoMailService zeptoMailService,
			@Value("${sms.base-url:}") String baseUrl,
			@Value("${sms.username:}") String username,
			@Value("${sms.password:}") String password) {
		this.restClient = restClient;
		this.zeptoMailService = zeptoMailService;
		this.baseUrl = trimSlash(baseUrl);
		this.username = username == null ? "" : username.trim();
		this.password = password == null ? "" : password.trim();
	}

	/** Tests that do not exercise Zepto failover. */
	public SmsGatewayService(RestClient restClient, String baseUrl, String username, String password) {
		this(restClient, null, baseUrl, username, password);
	}

	@Override
	public void sendOtp(String phoneNumber, String otpCode) {
		sendOtp(phoneNumber, otpCode, null);
	}

	@Override
	public boolean sendOtp(String phoneNumber, String otpCode, String fallbackEmail) {
		NotificationInputSanitizer.rejectCrLf(phoneNumber, "phone");
		String otp = NotificationInputSanitizer.requireOtp(otpCode);
		String phone = NotificationInputSanitizer.requireE164Phone(phoneNumber);

		if (baseUrl.isBlank() || username.isBlank() || password.isBlank()) {
			if (failover(fallbackEmail, otp, phone)) {
				return true;
			}
			throw new CustomException("SMS gateway is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}

		Map<String, Object> body = Map.of(
				"textMessage", Map.of("text",
						"Kittyp code " + otp + ". Valid 10 min."),
				"phoneNumbers", List.of(phone));

		try {
			String response = restClient.post()
					.uri(baseUrl + SEND_PATH)
					.header("Authorization", basicAuth(username, password))
					.header("Content-Type", "application/json")
					.body(body)
					.retrieve()
					.body(String.class);
			log.info("SMS OTP queued for {} smsgate={}", PiiMasker.maskPhone(phone), truncate(response));
			return false;
		} catch (RestClientResponseException e) {
			String snippet = e.getResponseBodyAsString();
			if (snippet != null && snippet.length() > 200) {
				snippet = snippet.substring(0, 200);
			}
			log.warn("SMS gateway HTTP {} for {}: {}", e.getStatusCode().value(), PiiMasker.maskPhone(phone), snippet);
			if (isServerError(e.getStatusCode()) && failover(fallbackEmail, otp, phone)) {
				return true;
			}
			throw new CustomException("Failed to send SMS", HttpStatus.BAD_GATEWAY, e);
		} catch (ResourceAccessException e) {
			log.warn("SMS gateway unreachable for {}: {}", PiiMasker.maskPhone(phone), e.getMessage());
			if (failover(fallbackEmail, otp, phone)) {
				return true;
			}
			throw new CustomException(
					"SMS gateway unreachable. Use email OTP, or start the SMS gateway and retry.",
					HttpStatus.SERVICE_UNAVAILABLE, e);
		}
	}

	private boolean failover(String fallbackEmail, String otp, String phone) {
		if (zeptoMailService == null || fallbackEmail == null || fallbackEmail.isBlank()) {
			return false;
		}
		NotificationInputSanitizer.requireEmail(fallbackEmail);
		zeptoMailService.sendSignupOtpEmail(fallbackEmail.trim(), otp, "PHONE", phone);
		log.warn("SMS gateway failed; OTP emailed to {}", PiiMasker.maskEmail(fallbackEmail));
		return true;
	}

	private static boolean isServerError(HttpStatusCode status) {
		return status != null && status.is5xxServerError();
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

	static String toE164(String phoneNumber) {
		return NotificationInputSanitizer.requireE164Phone(phoneNumber);
	}

	private static String truncate(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		String trimmed = body.trim();
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}
}
