package com.kittyp.notification.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

/**
 * Meta Embedded Signup: exchange auth code for a business token, subscribe WABA, register phone.
 */
@Slf4j
@Service
public class WhatsAppEmbeddedSignupService {

	private final ObjectMapper objectMapper;
	private final WhatsAppCredentialsVerifier credentialsVerifier;
	private final String apiVersion;
	private final String metaAppId;
	private final String metaAppSecret;
	private final String registerPin;
	private final String graphBaseUrl;

	@Autowired
	public WhatsAppEmbeddedSignupService(
			ObjectMapper objectMapper,
			WhatsAppCredentialsVerifier credentialsVerifier,
			@Value("${whatsapp.api-version:v21.0}") String apiVersion,
			@Value("${whatsapp.meta-app-id:}") String metaAppId,
			@Value("${whatsapp.meta-app-secret:}") String metaAppSecret,
			@Value("${whatsapp.register-pin:}") String registerPin) {
		this(objectMapper, credentialsVerifier, apiVersion, metaAppId, metaAppSecret, registerPin,
				"https://graph.facebook.com");
	}

	WhatsAppEmbeddedSignupService(
			ObjectMapper objectMapper,
			WhatsAppCredentialsVerifier credentialsVerifier,
			String apiVersion,
			String metaAppId,
			String metaAppSecret,
			String registerPin,
			String graphBaseUrl) {
		this.objectMapper = objectMapper;
		this.credentialsVerifier = credentialsVerifier;
		this.apiVersion = blank(apiVersion, "v21.0");
		this.metaAppId = metaAppId == null ? "" : metaAppId.trim();
		this.metaAppSecret = metaAppSecret == null ? "" : metaAppSecret.trim();
		this.registerPin = registerPin == null ? "" : registerPin.trim();
		this.graphBaseUrl = graphBaseUrl == null || graphBaseUrl.isBlank()
				? "https://graph.facebook.com"
				: graphBaseUrl.replaceAll("/$", "");
	}

	public record EmbeddedConnectResult(String accessToken, String wabaId, String phoneNumberId) {
	}

	/**
	 * Exchange Embedded Signup code, resolve WABA/phone, subscribe app, optionally register phone.
	 */
	public EmbeddedConnectResult complete(
			String authorizationCode,
			String wabaIdFromClient,
			String phoneNumberIdFromClient) {
		if (!StringUtils.hasText(metaAppId) || !StringUtils.hasText(metaAppSecret)) {
			throw new CustomException(
					"Embedded Signup is not configured on the server (missing Meta App ID/secret)",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		if (!StringUtils.hasText(authorizationCode)) {
			throw new CustomException("Authorization code is required", HttpStatus.BAD_REQUEST);
		}

		String accessToken = exchangeCode(authorizationCode.trim());
		String wabaId = StringUtils.hasText(wabaIdFromClient) ? wabaIdFromClient.trim() : null;
		String phoneNumberId = StringUtils.hasText(phoneNumberIdFromClient) ? phoneNumberIdFromClient.trim() : null;

		if (!StringUtils.hasText(wabaId) || !StringUtils.hasText(phoneNumberId)) {
			ResolvedAssets resolved = resolveFromToken(accessToken, wabaId, phoneNumberId);
			wabaId = resolved.wabaId();
			phoneNumberId = resolved.phoneNumberId();
		}

		credentialsVerifier.requirePhoneBelongsToWaba(accessToken, wabaId, phoneNumberId);
		subscribeApp(accessToken, wabaId);
		registerPhoneIfNeeded(accessToken, phoneNumberId);

		return new EmbeddedConnectResult(accessToken, wabaId, phoneNumberId);
	}

	private String exchangeCode(String code) {
		try {
			String body = RestClient.builder()
					.baseUrl(graphBaseUrl + "/" + apiVersion)
					.build()
					.get()
					.uri(uriBuilder -> uriBuilder
							.path("/oauth/access_token")
							.queryParam("client_id", metaAppId)
							.queryParam("client_secret", metaAppSecret)
							.queryParam("code", code)
							.build())
					.retrieve()
					.body(String.class);
			JsonNode node = objectMapper.readTree(body == null ? "{}" : body);
			String token = node.path("access_token").asText(null);
			if (!StringUtils.hasText(token)) {
				throw new CustomException("Meta did not return an access token from the signup code",
						HttpStatus.BAD_GATEWAY);
			}
			return token;
		} catch (CustomException e) {
			throw e;
		} catch (RestClientResponseException e) {
			throw new CustomException(
					"Failed to exchange WhatsApp signup code: " + metaDetail(e),
					HttpStatus.BAD_REQUEST,
					e);
		} catch (Exception e) {
			throw new CustomException("Failed to exchange WhatsApp signup code", HttpStatus.BAD_GATEWAY, e);
		}
	}

	private ResolvedAssets resolveFromToken(String accessToken, String preferredWaba, String preferredPhone) {
		try {
			String body = RestClient.builder()
					.baseUrl(graphBaseUrl + "/" + apiVersion)
					.defaultHeader("Authorization", "Bearer " + accessToken)
					.build()
					.get()
					.uri(uriBuilder -> uriBuilder
							.path("/debug_token")
							.queryParam("input_token", accessToken)
							.build())
					.retrieve()
					.body(String.class);
			JsonNode data = objectMapper.readTree(body == null ? "{}" : body).path("data");
			String wabaId = preferredWaba;
			if (!StringUtils.hasText(wabaId)) {
				JsonNode scopes = data.path("granular_scopes");
				if (scopes.isArray()) {
					for (JsonNode scope : scopes) {
						String scopeName = scope.path("scope").asText("");
						if (scopeName.contains("whatsapp_business")) {
							JsonNode targets = scope.path("target_ids");
							if (targets.isArray() && !targets.isEmpty()) {
								wabaId = targets.get(0).asText(null);
								break;
							}
						}
					}
				}
			}
			if (!StringUtils.hasText(wabaId)) {
				throw new CustomException(
						"Could not determine WhatsApp Business Account from signup. Retry Connect with Meta.",
						HttpStatus.BAD_REQUEST);
			}
			String phoneId = preferredPhone;
			if (!StringUtils.hasText(phoneId)) {
				JsonNode phones = credentialsVerifier.getJsonPublic(accessToken,
						"/" + wabaId + "/phone_numbers?fields=id");
				JsonNode list = phones.path("data");
				if (!list.isArray() || list.isEmpty()) {
					throw new CustomException(
							"No phone number on the connected WhatsApp Business Account yet",
							HttpStatus.BAD_REQUEST);
				}
				phoneId = list.get(0).path("id").asText(null);
			}
			if (!StringUtils.hasText(phoneId)) {
				throw new CustomException("Could not determine Phone Number ID from signup", HttpStatus.BAD_REQUEST);
			}
			return new ResolvedAssets(wabaId, phoneId);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException("Could not resolve WhatsApp assets from token", HttpStatus.BAD_GATEWAY, e);
		}
	}

	private void subscribeApp(String accessToken, String wabaId) {
		try {
			RestClient.builder()
					.baseUrl(graphBaseUrl + "/" + apiVersion)
					.defaultHeader("Authorization", "Bearer " + accessToken)
					.build()
					.post()
					.uri("/{wabaId}/subscribed_apps", wabaId)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of())
					.retrieve()
					.toBodilessEntity();
			log.info("Subscribed KittyP app to WABA {}", wabaId);
		} catch (RestClientResponseException e) {
			String detail = metaDetail(e);
			String lower = detail.toLowerCase();
			if (lower.contains("already") || e.getStatusCode().value() == 400) {
				log.info("WABA {} subscribe skipped: {}", wabaId, detail);
				return;
			}
			log.warn("WABA subscribe failed for {}: {}", wabaId, detail);
			// Non-fatal for sending; templates still work with token
		} catch (Exception e) {
			log.warn("WABA subscribe failed for {}: {}", wabaId, e.getMessage());
		}
	}

	private void registerPhoneIfNeeded(String accessToken, String phoneNumberId) {
		if (!StringUtils.hasText(registerPin) || registerPin.length() != 6) {
			log.info("Skipping phone register (whatsapp.register-pin not set to 6 digits)");
			return;
		}
		try {
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("messaging_product", "whatsapp");
			payload.put("pin", registerPin);
			RestClient.builder()
					.baseUrl(graphBaseUrl + "/" + apiVersion)
					.defaultHeader("Authorization", "Bearer " + accessToken)
					.build()
					.post()
					.uri("/{phoneNumberId}/register", phoneNumberId)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
			log.info("Registered phone {} for Cloud API", phoneNumberId);
		} catch (RestClientResponseException e) {
			String detail = metaDetail(e);
			if (detail.toLowerCase().contains("already") || e.getStatusCode().value() == 400) {
				log.info("Phone register skipped: {}", detail);
				return;
			}
			log.warn("Phone register failed: {}", detail);
		} catch (Exception e) {
			log.warn("Phone register failed: {}", e.getMessage());
		}
	}

	private String metaDetail(RestClientResponseException e) {
		try {
			JsonNode err = objectMapper.readTree(e.getResponseBodyAsString() == null ? "{}" : e.getResponseBodyAsString())
					.path("error");
			String message = err.path("message").asText(null);
			return StringUtils.hasText(message) ? message : ("HTTP " + e.getStatusCode().value());
		} catch (Exception ignored) {
			return "HTTP " + e.getStatusCode().value();
		}
	}

	private static String blank(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	private record ResolvedAssets(String wabaId, String phoneNumberId) {
	}
}
