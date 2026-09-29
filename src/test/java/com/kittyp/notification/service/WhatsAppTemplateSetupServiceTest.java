package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

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
						{"data":[{"name":"invoice_receipt","status":"APPROVED","language":"en"}]}
						""")
				.addHeader("Content-Type", "application/json"));

		Map<String, Object> status = service.templateStatus("tok", "waba-1");
		assertEquals(true, status.get("templatesReady"));
		assertEquals("READY", status.get("templatesStatus"));
	}

	@Test
	void ensureCreatesWhenMissing() throws Exception {
		// list empty
		server.enqueue(new MockResponse()
				.setBody("{\"data\":[]}")
				.addHeader("Content-Type", "application/json"));
		// upload session
		server.enqueue(new MockResponse()
				.setBody("{\"id\":\"upload:session1\"}")
				.addHeader("Content-Type", "application/json"));
		// upload binary → handle
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
						{"data":[{"name":"invoice_receipt","status":"PENDING","language":"en"}]}
						""")
				.addHeader("Content-Type", "application/json"));
		// vaccine create attempts: status from first list was empty — ensureText uses existingBefore
		// so it will try create for vaccine, checkup, promo — each create + list
		for (int i = 0; i < 3; i++) {
			server.enqueue(new MockResponse()
					.setBody("{\"id\":\"tmpl\"}")
					.addHeader("Content-Type", "application/json"));
			server.enqueue(new MockResponse()
					.setBody("{\"data\":[{\"name\":\"invoice_receipt\",\"status\":\"PENDING\",\"language\":\"en\"}]}")
					.addHeader("Content-Type", "application/json"));
		}

		Map<String, Object> result = service.ensureKittyPTemplates("tok", "waba-1");
		assertFalse(Boolean.TRUE.equals(result.get("templatesReady")));
		assertEquals("PENDING", result.get("templatesStatus"));
		assertTrue(String.valueOf(result.get("templatesMessage")).toLowerCase().contains("meta"));
	}
}
