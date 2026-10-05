package com.kittyp.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.support.service.InboundMail;
import com.kittyp.support.service.SupportMailParser;
import com.kittyp.support.service.SupportMailService;

class ZohoMailWebhookControllerTest {

	private static final String URL = "/api/v1/webhook/zoho-mail";
	private static final String BODY = "{\"messageId\":\"m-1\",\"fromAddress\":\"a@b.com\",\"subject\":\"Hi\",\"text\":\"hello\"}";

	private SupportMailService supportMailService;
	private SupportMailParser supportMailParser;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		supportMailService = mock(SupportMailService.class);
		supportMailParser = mock(SupportMailParser.class);
		ZohoMailWebhookController controller = new ZohoMailWebhookController(new ApiResponse<>(), supportMailService,
				supportMailParser);
		GlobalExceptionHandler advice = new GlobalExceptionHandler(new ApiResponse<>());
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
		stringConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(advice)
				.setMessageConverters(stringConverter, new MappingJackson2HttpMessageConverter())
				.build();
		when(supportMailParser.parse(anyString()))
				.thenReturn(new InboundMail("m-1", "a@b.com", "Hi", "hello", null));
	}

	@Test
	void hookSecretHandshakeEchoesHeaderAndDoesNotStoreMail() throws Exception {
		mockMvc.perform(post(URL)
				.contentType(MediaType.APPLICATION_JSON)
				.header(ZohoMailWebhookController.HOOK_SECRET_HEADER, "generated-secret")
				.content(BODY))
				.andExpect(status().isOk())
				.andExpect(header().string(ZohoMailWebhookController.HOOK_SECRET_HEADER, "generated-secret"));

		verify(supportMailService).acceptHookSecret("generated-secret");
		verify(supportMailService, never()).verifyHookSignature(anyString(), anyString());
		verify(supportMailService, never()).verifySecret(any());
		verify(supportMailParser, never()).parse(anyString());
		verify(supportMailService, never()).receive(any());
	}

	@Test
	void validHookSignatureProcessesMail() throws Exception {
		mockMvc.perform(post(URL)
				.contentType(MediaType.APPLICATION_JSON)
				.header(ZohoMailWebhookController.HOOK_SIGNATURE_HEADER, "c2lnbmF0dXJl")
				.content(BODY))
				.andExpect(status().isOk());

		verify(supportMailService).verifyHookSignature(BODY, "c2lnbmF0dXJl");
		verify(supportMailService).receive(any());
	}

	@Test
	void invalidHookSignatureDoesNotStoreMail() throws Exception {
		doThrow(new CustomException("Invalid webhook signature", HttpStatus.UNAUTHORIZED))
				.when(supportMailService).verifyHookSignature(anyString(), eq("bad"));

		mockMvc.perform(post(URL)
				.contentType(MediaType.APPLICATION_JSON)
				.header(ZohoMailWebhookController.HOOK_SIGNATURE_HEADER, "bad")
				.content(BODY))
				.andExpect(status().isUnauthorized());

		verify(supportMailParser, never()).parse(anyString());
		verify(supportMailService, never()).receive(any());
	}

	@Test
	void missingAuthenticationDoesNotStoreMail() throws Exception {
		doThrow(new CustomException("Missing webhook secret", HttpStatus.UNAUTHORIZED))
				.when(supportMailService).verifySecret(isNull());

		mockMvc.perform(post(URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BODY))
				.andExpect(status().isUnauthorized());

		verify(supportMailService).verifySecret(isNull());
		verify(supportMailParser, never()).parse(anyString());
		verify(supportMailService, never()).receive(any());
	}

	@Test
	void manualSecretStillProcessesMail() throws Exception {
		mockMvc.perform(post(URL)
				.contentType(MediaType.APPLICATION_JSON)
				.header(ZohoMailWebhookController.SECRET_HEADER, "top-secret")
				.content(BODY))
				.andExpect(status().isOk());

		verify(supportMailService).verifySecret("top-secret");
		verify(supportMailService, never()).verifyHookSignature(anyString(), anyString());
		verify(supportMailService).receive(any());
	}
}
