package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kittyp.common.exception.CustomException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class SmsGatewayServiceTest {

	private MockWebServer server;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
		objectMapper = new ObjectMapper();
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	void sendOtp_postsSmsGatePayload() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"id\":\"m1\"}"));
		SmsGatewayService service = newService("sms", "secret");

		service.sendOtp("+919876543210", "123456");

		RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
		assertEquals("POST", request.getMethod());
		assertEquals("/message", request.getPath());
		assertEquals(basic("sms", "secret"), request.getHeader("Authorization"));
		Map<String, Object> body = objectMapper.readValue(request.getBody().readUtf8(),
				new TypeReference<Map<String, Object>>() {
				});
		assertEquals(List.of("+919876543210"), body.get("phoneNumbers"));
		@SuppressWarnings("unchecked")
		Map<String, Object> textMessage = (Map<String, Object>) body.get("textMessage");
		assertTrue(String.valueOf(textMessage.get("text")).contains("123456"));
	}

	@Test
	void sendOtp_tenDigitLocalBecomesE164() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
		SmsGatewayService service = newService("sms", "secret");

		service.sendOtp("9876543210", "123456");

		RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
		Map<String, Object> body = objectMapper.readValue(request.getBody().readUtf8(),
				new TypeReference<Map<String, Object>>() {
				});
		assertEquals(List.of("+919876543210"), body.get("phoneNumbers"));
	}

	@Test
	void sendOtp_gateway500_throwsBadGateway() {
		server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
		SmsGatewayService service = newService("sms", "secret");

		CustomException ex = assertThrows(CustomException.class, () -> service.sendOtp("+919876543210", "123456"));
		assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
		assertEquals("Failed to send SMS", ex.getMessage());
	}

	@Test
	void sendOtp_missingPassword_doesNotCallGateway() throws Exception {
		SmsGatewayService service = newService("sms", "");

		CustomException ex = assertThrows(CustomException.class, () -> service.sendOtp("+919876543210", "123456"));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
		assertEquals(0, server.getRequestCount());
	}

	private SmsGatewayService newService(String username, String password) {
		String base = server.url("/").toString().replaceAll("/$", "");
		return new SmsGatewayService(RestClient.builder().build(), base, username, password);
	}

	private static String basic(String user, String pass) {
		return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
	}
}
