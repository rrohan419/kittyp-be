package com.kittyp.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.dto.ApiResponse;
import com.kittyp.common.exception.GlobalExceptionHandler;
import com.kittyp.common.util.Mapper;
import com.kittyp.payment.service.WebhookService;

class WebhookControllerSignatureTest {

	private static final String SECRET = "whsec_test_secret";
	private static final String PAYLOAD = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_1\",\"order_id\":\"order_1\",\"status\":\"captured\"}}}}";

	private WebhookService webhookService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		webhookService = mock(WebhookService.class);
		Mapper mapper = new Mapper(new ModelMapper(), new ObjectMapper(), mock(Environment.class));
		ApiResponse<?> responseBuilder = new ApiResponse<>();
		WebhookController controller = new WebhookController(responseBuilder, webhookService, mapper);
		ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
		GlobalExceptionHandler advice = new GlobalExceptionHandler(responseBuilder);

		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
		stringConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));

		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(advice)
				.setMessageConverters(stringConverter, new MappingJackson2HttpMessageConverter())
				.build();
	}

	@Test
	void missingSignature_unauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/webhook/razorpay")
				.contentType(MediaType.APPLICATION_JSON)
				.content(PAYLOAD))
				.andExpect(status().isUnauthorized());
		verify(webhookService, never()).razorpayWebbhook(any());
	}

	@Test
	void badSignature_unauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/webhook/razorpay")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Razorpay-Signature", "deadbeef")
				.content(PAYLOAD))
				.andExpect(status().isUnauthorized());
		verify(webhookService, never()).razorpayWebbhook(any());
	}

	@Test
	void validSignature_delegates() throws Exception {
		mockMvc.perform(post("/api/v1/webhook/razorpay")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Razorpay-Signature", hmac(PAYLOAD, SECRET))
				.content(PAYLOAD))
				.andExpect(status().isOk());

		verify(webhookService).razorpayWebbhook(any());
	}

	private static String hmac(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder(raw.length * 2);
		for (byte b : raw) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}
}
