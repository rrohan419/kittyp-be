package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class WhatsAppTemplateSetupServiceTest {

	private MockWebServer server;
	private WhatsAppTemplateSetupService service;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		service = new WhatsAppTemplateSetupService(
				new ObjectMapper(),
				"v21.0",
				"invoice",
				"en",
				"app-123",
				server.url("/").toString().replaceAll("/$", ""));
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void samplePdfIsNonEmpty() {
		assertTrue(WhatsAppTemplateSetupService.sampleInvoicePdf().length > 40);
	}

	@Test
	void templateStatusReportsApprovedInvoice() throws Exception {
		server.enqueue(new MockResponse()
				.setBody("""
						{"data":[{"name":"invoice","status":"APPROVED","language":"en"}]}
						""")
				.addHeader("Content-Type", "application/json"));

		Map<String, Object> status = service.templateStatus("tok", "waba-1");
		assertEquals(true, status.get("templatesReady"));
		assertEquals("READY", status.get("templatesStatus"));
	}

	@Test
	void listsOnlyApprovedTemplatesAndPreservesBodyVariablesAndDocumentHeader() throws Exception {
		server.enqueue(new MockResponse()
				.setBody("""
						{"data":[
						  {"name":"vaccine_reminder","status":"ACTIVE","language":"en","category":"UTILITY","components":[
						    {"type":"BODY","text":"Hello {{1}}, this is a reminder from {{3}} that {{2}} is due for {{4}} on {{5}}.\\n\\nPlease contact the clinic to schedule the vaccination.\\n\\nIf this vaccination has already been completed or an appointment is already booked, please ignore this reminder."}
						  ]},
						  {"name":"checkup_reminder","status":"APPROVED","language":"en","category":"UTILITY","components":[
						    {"type":"BODY","text":"Hello {{1}}, following {{2}}'s visit to {{3}} on {{4}}, the veterinarian recommended a follow-up check-up around {{5}}.\\n\\nPlease contact the clinic to arrange the visit.\\n\\nIf this follow-up is already booked, you can ignore this reminder."}
						  ]},
						  {"name":"invoice","status":"APPROVED","language":"en","category":"UTILITY","components":[
						    {"type":"HEADER","format":"DOCUMENT"},
						    {"type":"BODY","text":"Invoice {{1}} for {{2}}"}
						  ]},
						  {"name":"button_template","status":"APPROVED","language":"en","components":[
						    {"type":"BODY","text":"Hello {{1}}"},
						    {"type":"BUTTONS","buttons":[{"type":"URL","url":"https://example.test/{{1}}"}]}
						  ]},
						  {"name":"not_ready","status":"PENDING","language":"en","components":[
						    {"type":"BODY","text":"Pending {{1}}"}
						  ]},
						  {"name":"hello_world","status":"APPROVED","language":"en_US","category":"UTILITY","components":[
						    {"type":"HEADER","format":"TEXT","text":"Hello World"},
						    {"type":"BODY","text":"Sample WhatsApp message"}
						  ]}
						]}
						""")
				.addHeader("Content-Type", "application/json"));

		var templates = service.listApprovedMessagingTemplates("practice-token", "waba-1");

		assertEquals(3, templates.size());
		var vaccine = templates.stream().filter(template -> "vaccine_reminder".equals(template.name()))
				.findFirst().orElseThrow();
		assertEquals("Hello {{1}}, this is a reminder from {{3}} that {{2}} is due for {{4}} on {{5}}.\n\n"
				+ "Please contact the clinic to schedule the vaccination.\n\n"
				+ "If this vaccination has already been completed or an appointment is already booked, "
				+ "please ignore this reminder.", vaccine.body());
		assertEquals(java.util.List.of("1", "2", "3", "4", "5"), vaccine.variables());
		assertEquals("WHATSAPP", vaccine.channel());
		var checkup = templates.stream().filter(template -> "checkup_reminder".equals(template.name()))
				.findFirst().orElseThrow();
		assertEquals("Hello {{1}}, following {{2}}'s visit to {{3}} on {{4}}, the veterinarian recommended a "
				+ "follow-up check-up around {{5}}.\n\n"
				+ "Please contact the clinic to arrange the visit.\n\n"
				+ "If this follow-up is already booked, you can ignore this reminder.", checkup.body());
		assertEquals(java.util.List.of("1", "2", "3", "4", "5"), checkup.variables());
		var invoice = templates.stream().filter(template -> "invoice".equals(template.name()))
				.findFirst().orElseThrow();
		assertTrue(invoice.requiresDocument());
		assertNotNull(server.takeRequest());
	}

	@Test
	void reportsMetaRejectionInsteadOfAnEmptyTemplateList() {
		server.enqueue(new MockResponse()
				.setResponseCode(400)
				.setBody("{\"error\":{\"message\":\"Invalid access token\"}}")
				.addHeader("Content-Type", "application/json"));

		CustomException exception = assertThrows(CustomException.class,
				() -> service.listApprovedMessagingTemplates("token", "waba-1"));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
		assertTrue(exception.getMessage().contains("Invalid access token"));
		assertTrue(exception.getMessage().contains("WHATSAPP_BUSINESS_ACCOUNT_ID"));
		assertTrue(exception.getMessage().contains("whatsapp_business_management"));
	}

	@Test
	void ensureCreatesWhenMissing() throws Exception {
		// list empty
		server.enqueue(new MockResponse()
				.setBody("{\"data\":[]}")
				.addHeader("Content-Type", "application/json"));
		server.enqueue(new MockResponse()
				.setBody("{\"id\":\"upload:session1\"}")
				.addHeader("Content-Type", "application/json"));
		server.enqueue(new MockResponse()
				.setBody("{\"h\":\"handle-abc\"}")
				.addHeader("Content-Type", "application/json"));
		// create invoice template
		server.enqueue(new MockResponse()
				.setBody("{\"id\":\"tmpl1\"}")
				.addHeader("Content-Type", "application/json"));
		// list after invoice
		server.enqueue(new MockResponse()
				.setBody("""
						{"data":[{"name":"invoice","status":"PENDING","language":"en"}]}
						""")
				.addHeader("Content-Type", "application/json"));
		// vaccine create attempts: status from first list was empty — ensureText uses existingBefore
		// so it will try create for vaccine, checkup, promo — each create + list
		for (int i = 0; i < 3; i++) {
			server.enqueue(new MockResponse()
					.setBody("{\"id\":\"tmpl\"}")
					.addHeader("Content-Type", "application/json"));
			server.enqueue(new MockResponse()
					.setBody("{\"data\":[{\"name\":\"invoice\",\"status\":\"PENDING\",\"language\":\"en\"}]}")
					.addHeader("Content-Type", "application/json"));
		}

		Map<String, Object> result = service.ensureKittyPTemplates("tok", "waba-1");
		assertFalse(Boolean.TRUE.equals(result.get("templatesReady")));
		assertEquals("PENDING", result.get("templatesStatus"));
		assertTrue(String.valueOf(result.get("templatesMessage")).toLowerCase().contains("meta"));

		server.takeRequest();
		server.takeRequest();
		server.takeRequest();
		RecordedRequest invoiceCreate = server.takeRequest();
		assertEquals("POST", invoiceCreate.getMethod());
		var template = new ObjectMapper().readTree(invoiceCreate.getBody().readUtf8());
		assertEquals("invoice", template.path("name").asText());
		assertEquals(2, template.path("components").size());
		assertEquals("HEADER", template.path("components").get(0).path("type").asText());
		assertEquals("DOCUMENT", template.path("components").get(0).path("format").asText());
		assertEquals("BODY", template.path("components").get(1).path("type").asText());
		assertTrue(template.path("components").get(1).path("text").asText().contains("Amount: ₹{{5}}"));
	}
}
