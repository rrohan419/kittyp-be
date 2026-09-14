package com.kittyp.notification.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.clinic.entity.Clinic;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.doctor.dao.DoctorProfileDao;
import com.kittyp.doctor.entity.DoctorProfile;

import lombok.extern.slf4j.Slf4j;

/**
 * Exchanges a Facebook Login for Business Embedded Signup {@code code} for a
 * long-lived Graph token, subscribes the app to the WABA, verifies credentials,
 * and persists them on a doctor profile or clinic.
 */
@Slf4j
@Service
public class WhatsAppEmbeddedSignupService {

    static final String LOCAL_REDIRECT_URI = "https://localhost:8080/";

    record VerifiedCredentials(String token, String wabaId, String phoneNumberId) {
    }

    private final ObjectMapper objectMapper;
    private final WhatsAppCredentialsVerifier verifier;
    private final DoctorProfileDao doctorProfileDao;
    private final ClinicRepository clinicRepository;
    private final String apiVersion;
    private final String graphBaseUrl;
    private final String appId;
    private final String appSecret;

    @Autowired
    public WhatsAppEmbeddedSignupService(
            ObjectMapper objectMapper,
            WhatsAppCredentialsVerifier verifier,
            DoctorProfileDao doctorProfileDao,
            ClinicRepository clinicRepository,
            @Value("${whatsapp.api-version:v21.0}") String apiVersion,
            @Value("${whatsapp.meta.app-id:}") String appId,
            @Value("${whatsapp.meta.app-secret:}") String appSecret) {
        this(objectMapper, verifier, doctorProfileDao, clinicRepository, apiVersion,
                "https://graph.facebook.com", appId, appSecret);
    }

    /** Package-visible for unit tests against MockWebServer. */
    WhatsAppEmbeddedSignupService(
            ObjectMapper objectMapper,
            WhatsAppCredentialsVerifier verifier,
            DoctorProfileDao doctorProfileDao,
            ClinicRepository clinicRepository,
            String apiVersion,
            String graphBaseUrl,
            String appId,
            String appSecret) {
        this.objectMapper = objectMapper;
        this.verifier = verifier;
        this.doctorProfileDao = doctorProfileDao;
        this.clinicRepository = clinicRepository;
        this.apiVersion = apiVersion == null || apiVersion.isBlank() ? "v21.0" : apiVersion.trim();
        this.graphBaseUrl = graphBaseUrl == null || graphBaseUrl.isBlank()
                ? "https://graph.facebook.com"
                : graphBaseUrl.replaceAll("/$", "");
        this.appId = appId == null ? "" : appId.trim();
        this.appSecret = appSecret == null ? "" : appSecret.trim();
    }

    public Map<String, Object> complete(DoctorProfile profile, String code, String wabaId, String phoneNumberId) {
        if (profile == null) {
            throw new CustomException("Doctor profile not found", HttpStatus.NOT_FOUND);
        }
        VerifiedCredentials creds = exchangeAndVerify(code, wabaId, phoneNumberId);
        profile.setWhatsappPhoneNumberId(creds.phoneNumberId());
        profile.setWhatsappBusinessAccountId(creds.wabaId());
        profile.setWhatsappToken(creds.token());
        DoctorProfile saved = doctorProfileDao.save(profile);
        log.info("WhatsApp Embedded Signup saved for doctorUuid={} phoneNumberId={}",
                saved.getUuid(), creds.phoneNumberId());
        return WhatsAppSettingsSupport.publicView(
                saved.getWhatsappPhoneNumberId(),
                saved.getWhatsappBusinessAccountId(),
                saved.getWhatsappToken());
    }

    public Map<String, Object> complete(Clinic clinic, String code, String wabaId, String phoneNumberId) {
        if (clinic == null) {
            throw new CustomException("Clinic not found", HttpStatus.NOT_FOUND);
        }
        VerifiedCredentials creds = exchangeAndVerify(code, wabaId, phoneNumberId);
        clinic.setWhatsappPhoneNumberId(creds.phoneNumberId());
        clinic.setWhatsappBusinessAccountId(creds.wabaId());
        clinic.setWhatsappToken(creds.token());
        Clinic saved = clinicRepository.save(clinic);
        log.info("WhatsApp Embedded Signup saved for clinicUuid={} phoneNumberId={}",
                saved.getUuid(), creds.phoneNumberId());
        return WhatsAppSettingsSupport.publicView(
                saved.getWhatsappPhoneNumberId(),
                saved.getWhatsappBusinessAccountId(),
                saved.getWhatsappToken());
    }

    VerifiedCredentials exchangeAndVerify(String code, String wabaId, String phoneNumberId) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new CustomException("WhatsApp Meta app credentials are not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!StringUtils.hasText(code) || !StringUtils.hasText(wabaId) || !StringUtils.hasText(phoneNumberId)) {
            throw new CustomException("code, wabaId, and phoneNumberId are required", HttpStatus.BAD_REQUEST);
        }
        String trimmedCode = code.trim();
        String trimmedWaba = wabaId.trim();
        String trimmedPhone = phoneNumberId.trim();

        String shortLived = exchangeCode(trimmedCode, false);
        String token = exchangeLongLived(shortLived);
        subscribeApp(trimmedWaba, token);
        verifier.verifyOrThrow(token, trimmedPhone, trimmedWaba);
        return new VerifiedCredentials(token, trimmedWaba, trimmedPhone);
    }

    private String exchangeCode(String code, boolean includeRedirectUri) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/oauth/access_token")
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("code", code);
        if (includeRedirectUri) {
            builder.queryParam("redirect_uri", LOCAL_REDIRECT_URI);
        }
        try {
            return requireAccessToken(getJson(builder.toUriString()), "code exchange");
        } catch (RestClientResponseException e) {
            if (!includeRedirectUri && isRedirectUriError(e)) {
                log.warn("Meta code exchange asked for redirect_uri; retrying with {}", LOCAL_REDIRECT_URI);
                return exchangeCode(code, true);
            }
            throw graphFailure("WhatsApp code exchange failed", e);
        }
    }

    private String exchangeLongLived(String shortLived) {
        String path = UriComponentsBuilder.fromPath("/oauth/access_token")
                .queryParam("grant_type", "fb_exchange_token")
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("fb_exchange_token", shortLived)
                .toUriString();
        try {
            return requireAccessToken(getJson(path), "long-lived token");
        } catch (RestClientResponseException e) {
            throw graphFailure("WhatsApp long-lived token exchange failed", e);
        }
    }

    private void subscribeApp(String wabaId, String token) {
        try {
            RestClient.builder()
                    .baseUrl(graphBaseUrl + "/" + apiVersion)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build()
                    .post()
                    .uri("/" + wabaId + "/subscribed_apps")
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw graphFailure("WhatsApp subscribed_apps failed", e);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("WhatsApp subscribed_apps failed — could not reach Meta", HttpStatus.BAD_GATEWAY, e);
        }
    }

    private JsonNode getJson(String pathAndQuery) {
        String body = RestClient.builder()
                .baseUrl(graphBaseUrl + "/" + apiVersion)
                .build()
                .get()
                .uri(pathAndQuery)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("WhatsApp Graph response was not JSON", HttpStatus.BAD_GATEWAY, e);
        }
    }

    private String requireAccessToken(JsonNode node, String step) {
        String token = node.path("access_token").asText(null);
        if (!StringUtils.hasText(token)) {
            throw new CustomException("Meta " + step + " did not return access_token", HttpStatus.BAD_GATEWAY);
        }
        return token.trim();
    }

    private boolean isRedirectUriError(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return body != null && body.toLowerCase().contains("redirect_uri");
    }

    private CustomException graphFailure(String prefix, RestClientResponseException e) {
        log.warn("{}: {} {}", prefix, e.getStatusCode().value(), e.getResponseBodyAsString());
        String detail = extractMetaError(e.getResponseBodyAsString());
        return new CustomException(
                prefix + (detail != null ? ": " + detail : ""),
                HttpStatus.BAD_REQUEST,
                e);
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
