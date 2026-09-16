package com.kittyp.notification.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

class WhatsAppWebhookSecurityTest {

	private static final String SECRET = "meta-app-secret";
	private static final String BODY = "{\"entry\":[]}";

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
				.standaloneSetup(new WhatsAppWebhookController(new ObjectMapper(), "verify-me", SECRET))
				.setControllerAdvice(new WebhookExceptionHandler())
				.setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
				.build();
	}

	@Test
	void forgedSignature_returns401TextPlain() throws Exception {
		mockMvc.perform(post("/api/v1/whatsapp/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Hub-Signature-256", "sha256=deadbeef")
				.content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
	}

	@Test
	void validHmac_returns200() throws Exception {
		mockMvc.perform(post("/api/v1/whatsapp/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Hub-Signature-256", WhatsAppWebhookControllerTest.hmac(SECRET, BODY))
				.content(BODY))
				.andExpect(status().isOk());
	}

	@Test
	void blankSecret_returns401() throws Exception {
		MockMvc blank = MockMvcBuilders
				.standaloneSetup(new WhatsAppWebhookController(new ObjectMapper(), "verify-me", ""))
				.setControllerAdvice(new WebhookExceptionHandler())
				.setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
				.build();

		blank.perform(post("/api/v1/whatsapp/webhook")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Hub-Signature-256", WhatsAppWebhookControllerTest.hmac(SECRET, BODY))
				.content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
	}
}
