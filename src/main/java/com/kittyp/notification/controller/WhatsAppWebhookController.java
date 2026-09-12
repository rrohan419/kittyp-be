package com.kittyp.notification.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.constants.ApiUrl;
import com.kittyp.notification.service.WhatsAppConnectionService;
import com.kittyp.notification.service.WhatsAppTemplateSetupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Meta WhatsApp Cloud API webhooks (template status, etc.).
 */
@Slf4j
@RestController
@RequestMapping(ApiUrl.BASE_URL)
@RequiredArgsConstructor
public class WhatsAppWebhookController {

	private final ObjectMapper objectMapper;
	private final WhatsAppConnectionService connectionService;

	@Value("${whatsapp.webhook-verify-token:}")
	private String verifyToken;

	@GetMapping(ApiUrl.WEBHOOK_WHATSAPP)
	public ResponseEntity<String> verify(
			@RequestParam(name = "hub.mode", required = false) String mode,
			@RequestParam(name = "hub.verify_token", required = false) String token,
			@RequestParam(name = "hub.challenge", required = false) String challenge) {
		if ("subscribe".equals(mode)
				&& StringUtils.hasText(verifyToken)
				&& verifyToken.equals(token)
				&& StringUtils.hasText(challenge)) {
			return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
	}

	@PostMapping(ApiUrl.WEBHOOK_WHATSAPP)
	public ResponseEntity<Map<String, String>> receive(@RequestBody String rawBody) {
		try {
			JsonNode root = objectMapper.readTree(rawBody == null ? "{}" : rawBody);
			JsonNode entries = root.path("entry");
			if (entries.isArray()) {
				for (JsonNode entry : entries) {
					String wabaId = entry.path("id").asText(null);
					JsonNode changes = entry.path("changes");
					if (!changes.isArray()) {
						continue;
					}
					for (JsonNode change : changes) {
						String field = change.path("field").asText("");
						JsonNode value = change.path("value");
						if ("message_template_status_update".equals(field)
								|| "message_template_quality_update".equals(field)) {
							String name = value.path("message_template_name").asText(null);
							if (!StringUtils.hasText(name)) {
								name = value.path("message_template_id").asText(null);
							}
							// Prefer event status; Meta sends "event": APPROVED / REJECTED / …
							String status = value.path("event").asText(null);
							if (!StringUtils.hasText(status)) {
								status = value.path("message_template_status").asText(null);
							}
							if (!StringUtils.hasText(name)) {
								name = WhatsAppTemplateSetupService.INVOICE_RECEIPT;
							}
							connectionService.updateInvoiceTemplateStatusForWaba(wabaId, name, status);
							log.info("WhatsApp template webhook waba={} name={} status={}", wabaId, name, status);
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn("WhatsApp webhook parse skipped: {}", e.getMessage());
		}
		return ResponseEntity.ok(Map.of("status", "ok"));
	}
}
