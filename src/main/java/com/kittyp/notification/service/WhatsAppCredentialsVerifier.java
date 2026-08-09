package com.kittyp.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

/**
 * Verifies Meta Cloud API credentials before persisting doctor/clinic WhatsApp settings.
 * Independent of {@code whatsapp.enabled} so settings can be validated while sending is off.
 */
@Slf4j
@Service
public class WhatsAppCredentialsVerifier {

    private final ObjectMapper objectMapper;
    private final String apiVersion;
    private final String graphBaseUrl;

    public WhatsAppCredentialsVerifier(
            ObjectMapper objectMapper,
            @Value("${whatsapp.api-version:v21.0}") String apiVersion) {
        this(objectMapper, apiVersion, "https://graph.facebook.com");
    }

    /** Package-visible for unit tests against MockWebServer. */
    WhatsAppCredentialsVerifier(ObjectMapper objectMapper, String apiVersion, String graphBaseUrl) {
        this.objectMapper = objectMapper;
        this.apiVersion = apiVersion == null || apiVersion.isBlank() ? "v21.0" : apiVersion.trim();
        this.graphBaseUrl = graphBaseUrl == null || graphBaseUrl.isBlank()
                ? "https://graph.facebook.com"
                : graphBaseUrl.replaceAll("/$", "");
    }

    /**
     * Probe Graph API with candidate credentials. Throws {@link CustomException} on failure.
     */
    public void verifyOrThrow(String token, String phoneNumberId, String businessAccountId) {
        if (!StringUtils.hasText(token)) {
            throw new CustomException("Access token is required to verify WhatsApp credentials", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(phoneNumberId) || !StringUtils.hasText(businessAccountId)) {
            throw new CustomException("Phone Number ID and Business Account ID are required", HttpStatus.BAD_REQUEST);
        }
        String phoneId = phoneNumberId.trim();
        String wabaId = businessAccountId.trim();
        String bearer = token.trim();

        JsonNode phone = getJson(bearer, "/" + phoneId + "?fields=id,status,display_phone_number,verified_name");
        String status = phone.path("status").asText("").trim().toUpperCase();
        if (!status.isEmpty() && !"CONNECTED".equals(status)) {
            throw new CustomException(
                    "WhatsApp phone number status is " + status + " (expected CONNECTED). Fix in Meta WhatsApp Manager.",
                    HttpStatus.BAD_REQUEST);
        }
        String returnedId = phone.path("id").asText(null);
        if (StringUtils.hasText(returnedId) && !returnedId.equals(phoneId)) {
            throw new CustomException("Phone Number ID does not match Meta response", HttpStatus.BAD_REQUEST);
        }

        JsonNode waba = getJson(bearer, "/" + wabaId + "?fields=id");
        String wabaReturned = waba.path("id").asText(null);
        if (StringUtils.hasText(wabaReturned) && !wabaReturned.equals(wabaId)) {
            throw new CustomException("Business Account ID does not match Meta response", HttpStatus.BAD_REQUEST);
        }

        log.info("WhatsApp credentials verified for phoneNumberId={} status={}", phoneId,
                status.isEmpty() ? "unknown" : status);
    }

    private JsonNode getJson(String token, String pathAndQuery) {
        try {
            String body = RestClient.builder()
                    .baseUrl(graphBaseUrl + "/" + apiVersion)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build()
                    .get()
                    .uri(pathAndQuery)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (RestClientResponseException e) {
            log.warn("WhatsApp credential verify failed: {} {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            String detail = extractMetaError(e.getResponseBodyAsString());
            throw new CustomException(
                    "WhatsApp verification failed" + (detail != null ? ": " + detail : " — check Phone Number ID, WABA ID, and token"),
                    HttpStatus.BAD_REQUEST,
                    e);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("WhatsApp verification failed — could not reach Meta", HttpStatus.BAD_GATEWAY, e);
        }
    }

    private String extractMetaError(String responseBody) {
        try {
            JsonNode err = objectMapper.readTree(responseBody == null ? "{}" : responseBody).path("error");
            String message = err.path("message").asText(null);
            if (StringUtils.hasText(message)) {
                return message.length() > 180 ? message.substring(0, 180) + "…" : message;
            }
        } catch (Exception ignored) {
            // ignore parse errors
        }
        return null;
    }
}
