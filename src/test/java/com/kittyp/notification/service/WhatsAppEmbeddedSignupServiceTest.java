package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class WhatsAppEmbeddedSignupServiceTest {

	private MockWebServer server;
	private WhatsAppEmbeddedSignupService service;
	private WhatsAppCredentialsVerifier verifier;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
		String base = server.url("/").toString().replaceAll("/$", "");
		verifier = new WhatsAppCredentialsVerifier(new ObjectMapper(), "v21.0", base);
		service = new WhatsAppEmbeddedSignupService(
				new ObjectMapper(),
				verifier,
				"v21.0",
				"app-id",
				"app-secret",
				"",
				base);
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void exchangesCodeAndValidatesPhoneInWaba() throws Exception {
		server.enqueue(new MockResponse()
				.setBody("{\"access_token\":\"bis-token\"}")
				.addHeader("Content-Type", "application/json"));
		server.enqueue(new MockResponse()
				.setBody("{\"data\":[{\"id\":\"ph1\"},{\"id\":\"ph2\"}]}")
				.addHeader("Content-Type", "application/json"));
		server.enqueue(new MockResponse()
				.setBody("{\"success\":true}")
				.addHeader("Content-Type", "application/json"));

		WhatsAppEmbeddedSignupService.EmbeddedConnectResult result =
				service.complete("auth-code", "waba1", "ph1");

		assertEquals("bis-token", result.accessToken());
		assertEquals("waba1", result.wabaId());
		assertEquals("ph1", result.phoneNumberId());

		RecordedRequest oauth = server.takeRequest();
		assertTrue(oauth.getPath().contains("/oauth/access_token"));
		assertTrue(oauth.getPath().contains("client_id=app-id"));
		assertTrue(oauth.getPath().contains("code=auth-code"));
	}

	@Test
	void rejectsPhoneNotInWaba() {
		server.enqueue(new MockResponse()
				.setBody("{\"access_token\":\"bis-token\"}")
				.addHeader("Content-Type", "application/json"));
		server.enqueue(new MockResponse()
				.setBody("{\"data\":[{\"id\":\"other-phone\"}]}")
				.addHeader("Content-Type", "application/json"));

		CustomException ex = assertThrows(CustomException.class,
				() -> service.complete("auth-code", "waba1", "ph1"));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
		assertTrue(ex.getMessage().toLowerCase().contains("does not belong"));
	}

	@Test
	void requiresAppSecretConfigured() {
		WhatsAppEmbeddedSignupService unconfigured = new WhatsAppEmbeddedSignupService(
				new ObjectMapper(),
				verifier,
				"v21.0",
				"",
				"",
				"",
				server.url("/").toString().replaceAll("/$", ""));
		CustomException ex = assertThrows(CustomException.class,
				() -> unconfigured.complete("code", "waba", "ph"));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
	}
}
