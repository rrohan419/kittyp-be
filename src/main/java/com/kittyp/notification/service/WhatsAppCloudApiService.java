package com.kittyp.notification.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "whatsapp.enabled", havingValue = "true")
public class WhatsAppCloudApiService implements WhatsAppService {

    private final ObjectMapper objectMapper;
    private final String apiVersion;
    private final String defaultCountryCode;

    public WhatsAppCloudApiService(
            ObjectMapper objectMapper,
            @Value("${whatsapp.api-version:v21.0}") String apiVersion,
            @Value("${whatsapp.default-country-code:91}") String defaultCountryCode) {
        this.objectMapper = objectMapper;
        this.apiVersion = apiVersion == null || apiVersion.isBlank() ? "v21.0" : apiVersion.trim();
        this.defaultCountryCode = defaultCountryCode == null || defaultCountryCode.isBlank()
                ? "91"
                : defaultCountryCode.replace("+", "").trim();
    }

    @Override
    public boolean isConfigured(WhatsAppSenderCredentials sender) {
        return sender != null && sender.isConfigured();
    }

    @Override
    public String toE164Digits(String rawPhone) {
        return WhatsAppPhones.toE164Digits(rawPhone, defaultCountryCode);
    }

    @Override
    public String uploadDocumentPdf(WhatsAppSenderCredentials sender, byte[] pdfBytes, String filename) {
        WhatsAppSenderCredentials creds = requireSender(sender);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new CustomException("Invoice PDF is empty", HttpStatus.BAD_REQUEST);
        }
        String safeName = (filename == null || filename.isBlank()) ? "invoice.pdf" : filename;
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("messaging_product", "whatsapp");
        body.part("type", "application/pdf");
        body.part("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return safeName;
            }
        }).contentType(MediaType.APPLICATION_PDF);

        try {
            String response = client(creds).post()
                    .uri("/{phoneNumberId}/media", creds.phoneNumberId())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(response == null ? "{}" : response);
            String id = node.path("id").asText(null);
            if (!StringUtils.hasText(id)) {
                throw new CustomException("WhatsApp media upload did not return an id", HttpStatus.BAD_GATEWAY);
            }
            return id;
        } catch (RestClientResponseException e) {
            log.error("WhatsApp media upload failed: status={}", e.getStatusCode().value());
            throw new CustomException("WhatsApp media upload failed", HttpStatus.BAD_GATEWAY, e);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("WhatsApp media upload failed", HttpStatus.BAD_GATEWAY, e);
        }
    }

    @Override
    public void sendDocumentTemplate(
            WhatsAppSenderCredentials sender,
            String toE164Digits,
            String templateName,
            String languageCode,
            String mediaId,
            String filename,
            List<String> bodyParams) {
        WhatsAppSenderCredentials creds = requireSender(sender);
        Map<String, Object> headerParam = new LinkedHashMap<>();
        headerParam.put("type", "document");
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("id", mediaId);
        document.put("filename", filename == null || filename.isBlank() ? "invoice.pdf" : filename);
        headerParam.put("document", document);

        Map<String, Object> headerComponent = new LinkedHashMap<>();
        headerComponent.put("type", "header");
        headerComponent.put("parameters", List.of(headerParam));

        List<Map<String, Object>> components = new ArrayList<>();
        components.add(headerComponent);
        if (bodyParams != null && !bodyParams.isEmpty()) {
            List<Map<String, Object>> params = new ArrayList<>();
            for (String p : bodyParams) {
                Map<String, Object> tp = new LinkedHashMap<>();
                tp.put("type", "text");
                tp.put("text", WhatsAppPhones.sanitizeTemplateText(p));
                params.add(tp);
            }
            Map<String, Object> bodyComponent = new LinkedHashMap<>();
            bodyComponent.put("type", "body");
            bodyComponent.put("parameters", params);
            components.add(bodyComponent);
        }

        Map<String, Object> language = Map.of("code", languageCode == null || languageCode.isBlank() ? "en" : languageCode);
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", language);
        template.put("components", components);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toE164Digits);
        payload.put("type", "template");
        payload.put("template", template);

        postMessage(creds, payload);
    }

    @Override
    public void sendTextTemplate(
            WhatsAppSenderCredentials sender,
            String toE164Digits,
            String templateName,
            String languageCode,
            List<String> bodyParams) {
        WhatsAppSenderCredentials creds = requireSender(sender);
        List<Map<String, Object>> components = new ArrayList<>();
        if (bodyParams != null && !bodyParams.isEmpty()) {
            List<Map<String, Object>> params = new ArrayList<>();
            for (String p : bodyParams) {
                Map<String, Object> tp = new LinkedHashMap<>();
                tp.put("type", "text");
                tp.put("text", WhatsAppPhones.sanitizeTemplateText(p));
                params.add(tp);
            }
            Map<String, Object> bodyComponent = new LinkedHashMap<>();
            bodyComponent.put("type", "body");
            bodyComponent.put("parameters", params);
            components.add(bodyComponent);
        }

        Map<String, Object> language = Map.of("code", languageCode == null || languageCode.isBlank() ? "en" : languageCode);
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", language);
        if (!components.isEmpty()) {
            template.put("components", components);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toE164Digits);
        payload.put("type", "template");
        payload.put("template", template);

        postMessage(creds, payload);
    }

    private void postMessage(WhatsAppSenderCredentials creds, Map<String, Object> payload) {
        try {
            client(creds).post()
                    .uri("/{phoneNumberId}/messages", creds.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp template message accepted for {}",
                    WhatsAppPhones.redact(String.valueOf(payload.get("to"))));
        } catch (RestClientResponseException e) {
            log.error("WhatsApp send failed: status={} to={}",
                    e.getStatusCode().value(),
                    WhatsAppPhones.redact(String.valueOf(payload.get("to"))));
            throw new CustomException("WhatsApp send failed: " + e.getStatusCode().value(), HttpStatus.BAD_GATEWAY, e);
        } catch (Exception e) {
            throw new CustomException("WhatsApp send failed", HttpStatus.BAD_GATEWAY, e);
        }
    }

    private RestClient client(WhatsAppSenderCredentials creds) {
        return RestClient.builder()
                .baseUrl("https://graph.facebook.com/" + apiVersion)
                .defaultHeader("Authorization", "Bearer " + creds.token())
                .build();
    }

    private WhatsAppSenderCredentials requireSender(WhatsAppSenderCredentials sender) {
        if (sender == null || !sender.isConfigured()) {
            throw new CustomException(
                    "WhatsApp is not configured for this account. Add Meta Phone Number ID and token in settings.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return sender;
    }
}
