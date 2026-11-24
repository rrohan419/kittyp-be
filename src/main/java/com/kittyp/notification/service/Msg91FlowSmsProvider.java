package com.kittyp.notification.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;
import com.kittyp.notification.entity.PracticeMessage;

/** Server-configured MSG91 Flow API. Clinic payloads can supply placeholder values only. */
@Component
public class Msg91FlowSmsProvider {
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public Msg91FlowSmsProvider(Environment environment,ObjectMapper objectMapper,
            @Qualifier("smsRestClient") RestClient restClient) {
        this.environment=environment;
        this.objectMapper=objectMapper;
        this.restClient=restClient;
    }

    public String send(PracticeMessage message, Map<String,Object> variables) {
        String auth = environment.getProperty("messaging.sms.msg91.auth-key",
                environment.getProperty("msg91.auth.key", ""));
        String flowId = propertyFor(message.getTemplateKey(), "flow-id");
        String sender = environment.getProperty("messaging.sms.msg91.sender-id", "");
        String dltTemplateId = propertyFor(message.getTemplateKey(), "dlt-template-id");
        String peId = environment.getProperty("messaging.sms.msg91.pe-id", "");
        if (!StringUtils.hasText(auth) || !StringUtils.hasText(flowId) || !StringUtils.hasText(sender)
                || !StringUtils.hasText(dltTemplateId) || !StringUtils.hasText(peId)) {
            throw new CustomException("SMS delivery unavailable: MSG91 AuthKey, Flow, sender and DLT settings must be configured",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        String url = environment.getProperty("messaging.sms.msg91.flow-url",
                "https://control.msg91.com/api/v5/flow/");
        String phone = message.getRecipient().replaceAll("[^0-9]", "");
        if (phone.length() < 10 || phone.length() > 15) {
            throw new CustomException("SMS recipient number is invalid", HttpStatus.BAD_REQUEST);
        }
        Map<String,Object> recipient = new LinkedHashMap<>();
        recipient.put("mobiles", phone);
        // MSG91 dynamic variables correspond to approved Flow placeholders; never accepts a free-form body.
        if (variables != null) {
            for (Map.Entry<String,Object> entry : variables.entrySet()) {
                if (!entry.getKey().matches("[A-Za-z0-9_]{1,40}") || entry.getValue() == null) continue;
                recipient.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("template_id", flowId);
        payload.put("sender", sender);
        payload.put("DLT_TE_ID", dltTemplateId);
        payload.put("DLT_PE_ID", peId);
        payload.put("recipients", java.util.List.of(recipient));
        try {
            String response = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .header("authkey", auth).body(payload).retrieve().body(String.class);
            JsonNode parsed = objectMapper.readTree(response == null ? "{}" : response);
            String id = parsed.path("request_id").asText(null);
            return StringUtils.hasText(id) ? id : null;
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            // Provider response body may include destination/content. Do not copy it to logs or client errors.
            throw new CustomException("MSG91 rejected SMS delivery (HTTP " + ex.getStatusCode().value() + ")",
                    HttpStatus.BAD_GATEWAY);
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException("MSG91 SMS delivery failed", HttpStatus.BAD_GATEWAY);
        }
    }

    private String propertyFor(String template, String suffix) {
        if (!StringUtils.hasText(template) || !template.matches("[A-Za-z0-9_-]{1,100}")) return "";
        return environment.getProperty("messaging.sms.templates." + template + "." + suffix, "");
    }
}
