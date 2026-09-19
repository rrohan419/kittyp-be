package com.kittyp.notification.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.constants.TemplateConstant;
import com.kittyp.common.util.PiiMasker;

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
    private final Environment env;

	@Autowired
	public SmsGatewayService(
			@Qualifier("smsRestClient") RestClient restClient,
			@Value("${sms.base-url:}") String baseUrl,
			@Value("${sms.username:}") String username,
			@Value("${sms.password:}") String password,
			Environment env) {
		this.restClient = restClient;
		this.baseUrl = baseUrl;
		this.username = username == null ? "" : username.trim();
		this.password = password == null ? "" : password.trim();
        this.env = env;
	}

	@Override
	public void sendOtp(String phoneNumber, String otpCode) {
		if (baseUrl.isBlank() || username.isBlank() || password.isBlank()) {
			
			throw new CustomException("SMS gateway properties are not configured", HttpStatus.SERVICE_UNAVAILABLE);
		}
		String template = env.getProperty(TemplateConstant .SMS_GATEWAY_OTP_TEMPLATE);
		String message = String.format(template, otpCode);
        log.info("");

        Map<String, Object> body = Map.of(
            "textMessage", Map.of("text", message),
            "phoneNumbers", List.of(phoneNumber));

    try {
        String response = restClient.post()
                .uri(baseUrl + SEND_PATH)
                .header("Authorization", basicAuth(username, password))
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);
        log.info("SMS OTP queued for {} smsgate={}", PiiMasker.maskPhone(phoneNumber), truncate(response));
    } catch (RestClientResponseException e) {
        String snippet = e.getResponseBodyAsString();
        if (snippet != null && snippet.length() > 200) {
            snippet = snippet.substring(0, 200);
        }
        log.warn("SMS gateway HTTP {} for {}: {}", e.getStatusCode().value(), PiiMasker.maskPhone(phoneNumber), snippet);
        
        throw new CustomException("Failed to send SMS", HttpStatus.BAD_GATEWAY, e);
    } catch (ResourceAccessException e) {
        log.warn("SMS gateway unreachable for {}: {}", PiiMasker.maskPhone(phoneNumber), e.getMessage());
        throw new CustomException(
                "SMS gateway unreachable. Check email for a Phone verify OTP, or start the SMS gateway and retry.",
                HttpStatus.SERVICE_UNAVAILABLE, e);
    }
		
	}

    private static String truncate(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		String trimmed = body.trim();
		return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
	}

    private static String basicAuth(String user, String pass) {
		String token = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
	}

	
}
