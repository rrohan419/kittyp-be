package com.kittyp.notification.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Creates KittyP-required WhatsApp message templates on a clinic/doctor WABA via Meta Graph API,
 * so admins only need Phone Number ID + WABA ID + token in KittyP settings.
 */
@Slf4j
@Service
public class WhatsAppTemplateSetupService {

	public static final String INVOICE_RECEIPT = "invoice_receipt";

	private final ObjectMapper objectMapper;
	private final String apiVersion;
	private final String invoiceTemplateLang;
	private final String metaAppId;
	private final String graphBaseUrl;

	@Autowired
	public WhatsAppTemplateSetupService(
			ObjectMapper objectMapper,
			@Value("${whatsapp.api-version:v21.0}") String apiVersion,
			@Value("${whatsapp.invoice-template-lang:en}") String invoiceTemplateLang,
			@Value("${whatsapp.meta-app-id:}") String metaAppId) {
		this(objectMapper, apiVersion, invoiceTemplateLang, metaAppId, "https://graph.facebook.com");
	}

	/** Package-visible for tests. */
	WhatsAppTemplateSetupService(
			ObjectMapper objectMapper,
			String apiVersion,
			String invoiceTemplateLang,
			String metaAppId,
			String graphBaseUrl) {
		this.objectMapper = objectMapper;
		this.apiVersion = blankToDefault(apiVersion, "v21.0");
		this.invoiceTemplateLang = blankToDefault(invoiceTemplateLang, "en");
		this.metaAppId = metaAppId == null ? "" : metaAppId.trim();
		this.graphBaseUrl = graphBaseUrl == null || graphBaseUrl.isBlank()
				? "https://graph.facebook.com"
				: graphBaseUrl.replaceAll("/$", "");
	}

	/**
	 * Ensure invoice (and optional reminder) templates exist on the WABA. Never throws for
	 * "already exists"; returns a status map for the settings UI.
	 */
	public Map<String, Object> ensureKittyPTemplates(String token, String businessAccountId) {
		if (!StringUtils.hasText(token) || !StringUtils.hasText(businessAccountId)) {
			return templateSummary(null, "MISSING_CREDENTIALS", "WhatsApp credentials incomplete", List.of());
		}
		String wabaId = businessAccountId.trim();
		String bearer = token.trim();

		List<Map<String, Object>> templates = new ArrayList<>();
		try {
			JsonNode existing = listTemplates(bearer, wabaId);
			String invoiceStatus = findTemplateStatus(existing, INVOICE_RECEIPT, invoiceTemplateLang);
			if (isUsable(invoiceStatus)) {
				templates.add(templateRow(INVOICE_RECEIPT, invoiceStatus, "Already on WhatsApp Business Account"));
			} else if ("PENDING".equalsIgnoreCase(invoiceStatus) || "IN_APPEAL".equalsIgnoreCase(invoiceStatus)) {
				templates.add(templateRow(INVOICE_RECEIPT, invoiceStatus,
						"Submitted — waiting for Meta approval (usually minutes to a day)"));
			} else {
				createInvoiceReceipt(bearer, wabaId);
				JsonNode after = listTemplates(bearer, wabaId);
				String status = findTemplateStatus(after, INVOICE_RECEIPT, invoiceTemplateLang);
				templates.add(templateRow(INVOICE_RECEIPT,
						status != null ? status : "PENDING",
						"Created on your WhatsApp Business Account — Meta must approve before sending"));
			}

			ensureTextTemplate(bearer, wabaId, existing, templates, "vaccine_reminder",
					"Reminder: {{1}} is due for {{2}}. Reply if you need to reschedule.",
					List.of(List.of("Buddy", "Rabies vaccine")));
			ensureTextTemplate(bearer, wabaId, existing, templates, "checkup_reminder",
					"Reminder: {{1}} has a checkup at {{2}} on {{3}}.",
					List.of(List.of("Buddy", "KittyP Clinic", "12 Sep 2026")));
			ensureTextTemplate(bearer, wabaId, existing, templates, "promo_offer",
					"{{1}} — special offer from your clinic. Message us to learn more.",
					List.of(List.of("Spring wellness package")));

			boolean invoiceReady = templates.stream()
					.anyMatch(t -> INVOICE_RECEIPT.equals(t.get("name")) && isUsable(String.valueOf(t.get("status"))));
			String overall = invoiceReady ? "READY"
					: templates.stream().anyMatch(t -> INVOICE_RECEIPT.equals(t.get("name"))
							&& "REJECTED".equalsIgnoreCase(String.valueOf(t.get("status"))))
							? "REJECTED"
							: "PENDING";
			String message = switch (overall) {
				case "READY" -> "Invoice WhatsApp template is approved — you can send invoices.";
				case "REJECTED" -> "Invoice template was rejected by Meta. Open WhatsApp Manager to fix, then retry setup.";
				default -> "Templates submitted. You can send invoices after Meta marks invoice_receipt as Approved.";
			};
			return templateSummary(overall, overall, message, templates);
		} catch (CustomException e) {
			log.warn("WhatsApp template setup failed: {}", e.getMessage());
			return templateSummary("ERROR", "ERROR", e.getMessage(), templates);
		} catch (Exception e) {
			log.error("WhatsApp template setup failed", e);
			return templateSummary("ERROR", "ERROR",
					"Could not create WhatsApp templates: " + e.getMessage(), templates);
		}
	}

	public Map<String, Object> templateStatus(String token, String businessAccountId) {
		if (!StringUtils.hasText(token) || !StringUtils.hasText(businessAccountId)) {
			return templateSummary(null, "MISSING_CREDENTIALS", "WhatsApp credentials incomplete", List.of());
		}
		try {
			JsonNode existing = listTemplates(token.trim(), businessAccountId.trim());
			List<Map<String, Object>> templates = new ArrayList<>();
			for (String name : List.of(INVOICE_RECEIPT, "vaccine_reminder", "checkup_reminder", "promo_offer")) {
				String status = findTemplateStatus(existing, name, invoiceTemplateLang);
				if (status != null) {
					templates.add(templateRow(name, status, null));
				}
			}
			String invoiceStatus = findTemplateStatus(existing, INVOICE_RECEIPT, invoiceTemplateLang);
			String overall = isUsable(invoiceStatus) ? "READY"
					: invoiceStatus == null ? "MISSING"
							: invoiceStatus.toUpperCase();
			String message = isUsable(invoiceStatus)
					? "Invoice template approved"
					: invoiceStatus == null
							? "Invoice template not found — save WhatsApp settings or retry setup"
							: "Invoice template status: " + invoiceStatus;
			return templateSummary(overall, overall, message, templates);
		} catch (CustomException e) {
			return templateSummary("ERROR", "ERROR", e.getMessage(), List.of());
		} catch (Exception e) {
			return templateSummary("ERROR", "ERROR", e.getMessage(), List.of());
		}
	}

	private void ensureTextTemplate(
			String bearer,
			String wabaId,
			JsonNode existingBefore,
			List<Map<String, Object>> out,
			String name,
			String bodyText,
			List<List<String>> bodyExamples) {
		try {
			String status = findTemplateStatus(existingBefore, name, invoiceTemplateLang);
			if (isUsable(status) || "PENDING".equalsIgnoreCase(status) || "IN_APPEAL".equalsIgnoreCase(status)) {
				out.add(templateRow(name, status, null));
				return;
			}
			createTextUtilityTemplate(bearer, wabaId, name, bodyText, bodyExamples);
			JsonNode after = listTemplates(bearer, wabaId);
			String afterStatus = findTemplateStatus(after, name, invoiceTemplateLang);
			out.add(templateRow(name, afterStatus != null ? afterStatus : "PENDING", "Created"));
		} catch (Exception e) {
			log.warn("Optional template {} setup skipped: {}", name, e.getMessage());
			out.add(templateRow(name, "SKIPPED", e.getMessage()));
		}
	}

	private void createInvoiceReceipt(String bearer, String wabaId) {
		String handle = uploadSamplePdfHandle(bearer);
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("type", "HEADER");
		header.put("format", "DOCUMENT");
		header.put("example", Map.of("header_handle", List.of(handle)));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "BODY");
		body.put("text",
				"Hi {{1}},\n\nHere is your treatment invoice from {{2}}.\n\nPet: {{3}}\nInvoice: {{4}}\nAmount: ₹{{5}}\n\nThank you for choosing us.");
		body.put("example", Map.of("body_text", List.of(
				List.of("Ada Sharma", "KittyP Clinic", "Buddy", "INV-1001", "1500.00"))));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", INVOICE_RECEIPT);
		payload.put("language", invoiceTemplateLang);
		payload.put("category", "UTILITY");
		payload.put("allow_category_change", true);
		payload.put("components", List.of(header, body));

		postTemplate(bearer, wabaId, payload);
	}

	private void createTextUtilityTemplate(
			String bearer,
			String wabaId,
			String name,
			String bodyText,
			List<List<String>> bodyExamples) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "BODY");
		body.put("text", bodyText);
		body.put("example", Map.of("body_text", bodyExamples));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", name);
		payload.put("language", invoiceTemplateLang);
		payload.put("category", "UTILITY");
		payload.put("allow_category_change", true);
		payload.put("components", List.of(body));
		postTemplate(bearer, wabaId, payload);
	}

	private void postTemplate(String bearer, String wabaId, Map<String, Object> payload) {
		try {
			client(bearer).post()
					.uri("/{wabaId}/message_templates", wabaId)
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);
			log.info("Created WhatsApp template {} on WABA {}", payload.get("name"), wabaId);
		} catch (RestClientResponseException e) {
			String detail = extractMetaError(e.getResponseBodyAsString());
			String lower = detail == null ? "" : detail.toLowerCase();
			// Idempotent: already exists
			if (e.getStatusCode().value() == 400 && (lower.contains("already exists")
					|| lower.contains("content is identical")
					|| lower.contains("duplicate"))) {
				log.info("WhatsApp template {} already exists on WABA {}", payload.get("name"), wabaId);
				return;
			}
			throw new CustomException(
					"Meta template create failed for " + payload.get("name")
							+ (detail != null ? ": " + detail : " (HTTP " + e.getStatusCode().value() + ")"),
					HttpStatus.BAD_GATEWAY,
					e);
		}
	}

	/**
	 * Meta DOCUMENT templates require a sample file handle from the Resumable Upload API (needs App ID).
	 */
	private String uploadSamplePdfHandle(String bearer) {
		if (!StringUtils.hasText(metaAppId)) {
			throw new CustomException(
					"Server missing whatsapp.meta-app-id (Meta App ID). Set WHATSAPP_META_APP_ID so KittyP can upload a sample PDF and create the invoice template.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		byte[] pdf = sampleInvoicePdf();
		try {
			String sessionJson = client(bearer).post()
					.uri(uriBuilder -> uriBuilder
							.path("/{appId}/uploads")
							.queryParam("file_name", "kittyp_invoice_sample.pdf")
							.queryParam("file_length", pdf.length)
							.queryParam("file_type", "application/pdf")
							.build(metaAppId))
					.retrieve()
					.body(String.class);
			JsonNode session = objectMapper.readTree(sessionJson == null ? "{}" : sessionJson);
			String uploadId = session.path("id").asText(null);
			if (!StringUtils.hasText(uploadId)) {
				throw new CustomException("Meta upload session did not return an id", HttpStatus.BAD_GATEWAY);
			}
			// id is like "upload:xxxxx" — post to /upload:xxxxx
			String uploadPath = uploadId.startsWith("upload:") ? "/" + uploadId : "/upload:" + uploadId;
			String handleJson = RestClient.builder()
					.baseUrl(graphBaseUrl + "/" + apiVersion)
					.defaultHeader("Authorization", "OAuth " + bearer)
					.defaultHeader("file_offset", "0")
					.build()
					.post()
					.uri(uploadPath)
					.contentType(MediaType.APPLICATION_PDF)
					.body(pdf)
					.retrieve()
					.body(String.class);
			JsonNode handleNode = objectMapper.readTree(handleJson == null ? "{}" : handleJson);
			String handle = handleNode.path("h").asText(null);
			if (!StringUtils.hasText(handle)) {
				throw new CustomException("Meta upload did not return a file handle", HttpStatus.BAD_GATEWAY);
			}
			return handle;
		} catch (CustomException e) {
			throw e;
		} catch (RestClientResponseException e) {
			String detail = extractMetaError(e.getResponseBodyAsString());
			throw new CustomException(
					"Meta sample PDF upload failed"
							+ (detail != null ? ": " + detail : " (HTTP " + e.getStatusCode().value() + ")")
							+ ". Token needs permission to upload; App ID must match the app that issued the token.",
					HttpStatus.BAD_GATEWAY,
					e);
		} catch (Exception e) {
			throw new CustomException("Meta sample PDF upload failed", HttpStatus.BAD_GATEWAY, e);
		}
	}

	private JsonNode listTemplates(String bearer, String wabaId) {
		try {
			String body = client(bearer).get()
					.uri(uriBuilder -> uriBuilder
							.path("/{wabaId}/message_templates")
							.queryParam("limit", 100)
							.queryParam("fields", "name,status,language")
							.build(wabaId))
					.retrieve()
					.body(String.class);
			return objectMapper.readTree(body == null ? "{}" : body);
		} catch (RestClientResponseException e) {
			String detail = extractMetaError(e.getResponseBodyAsString());
			throw new CustomException(
					"Could not list WhatsApp templates"
							+ (detail != null ? ": " + detail : "")
							+ ". Token needs whatsapp_business_management permission.",
					HttpStatus.BAD_REQUEST,
					e);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomException("Could not list WhatsApp templates", HttpStatus.BAD_GATEWAY, e);
		}
	}

	private static String findTemplateStatus(JsonNode listResponse, String name, String language) {
		if (listResponse == null) {
			return null;
		}
		JsonNode data = listResponse.path("data");
		if (!data.isArray()) {
			return null;
		}
		String lang = language == null ? "en" : language;
		String fallback = null;
		for (JsonNode row : data) {
			if (!name.equalsIgnoreCase(row.path("name").asText())) {
				continue;
			}
			String rowLang = row.path("language").asText("");
			String status = row.path("status").asText(null);
			if (lang.equalsIgnoreCase(rowLang) || lang.replace('_', '-').equalsIgnoreCase(rowLang.replace('_', '-'))) {
				return status;
			}
			if (fallback == null) {
				fallback = status;
			}
		}
		return fallback;
	}

	private static boolean isUsable(String status) {
		return status != null && ("APPROVED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status));
	}

	private RestClient client(String bearer) {
		return RestClient.builder()
				.baseUrl(graphBaseUrl + "/" + apiVersion)
				.defaultHeader("Authorization", "Bearer " + bearer)
				.build();
	}

	private String extractMetaError(String responseBody) {
		try {
			JsonNode err = objectMapper.readTree(responseBody == null ? "{}" : responseBody).path("error");
			String message = err.path("message").asText(null);
			if (!StringUtils.hasText(message)) {
				return null;
			}
			return message.length() > 220 ? message.substring(0, 220) + "…" : message;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static Map<String, Object> templateSummary(
			String overall, String status, String message, List<Map<String, Object>> templates) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("templatesStatus", status != null ? status : "UNKNOWN");
		map.put("templatesReady", "READY".equalsIgnoreCase(overall));
		map.put("templatesMessage", message != null ? message : "");
		map.put("templates", templates != null ? templates : List.of());
		return map;
	}

	private static Map<String, Object> templateRow(String name, String status, String detail) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("name", name);
		row.put("status", status != null ? status : "UNKNOWN");
		if (detail != null) {
			row.put("detail", detail);
		}
		return row;
	}

	private static String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	/** Minimal valid PDF used only as Meta template sample (not shown to pet parents). */
	static byte[] sampleInvoicePdf() {
		String pdf = """
				%PDF-1.4
				1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
				2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
				3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >> >> >>endobj
				4 0 obj<< /Length 68 >>stream
				BT /F1 24 Tf 72 720 Td (KittyP sample invoice) Tj ET
				endstream
				endobj
				5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj
				xref
				0 6
				0000000000 65535 f\s
				0000000009 00000 n\s
				0000000058 00000 n\s
				0000000115 00000 n\s
				0000000266 00000 n\s
				0000000384 00000 n\s
				trailer<< /Size 6 /Root 1 0 R >>
				startxref
				461
				%%EOF
				""";
		return pdf.getBytes(StandardCharsets.US_ASCII);
	}
}
