package com.kittyp.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	void sendOtp_postsJsonPayload() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}"));
		SmsGatewayService service = newService("tb-key", "device-1");

		service.sendOtp("+919876543210", "123456");

		RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
		assertEquals("POST", request.getMethod());
		assertEquals("/gateway/send-sms", request.getPath());
		assertEquals("tb-key", request.getHeader("x-api-key"));
		Map<String, Object> body = objectMapper.readValue(request.getBody().readUtf8(),
				new TypeReference<Map<String, Object>>() {
				});
		assertEquals("device-1", body.get("deviceId"));
		assertEquals(List.of("+919876543210"), body.get("recipients"));
		assertTrue(String.valueOf(body.get("message")).contains("123456"));
	}

	@Test
	void sendOtp_gateway500_throwsBadGateway() {
		server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
		SmsGatewayService service = newService("tb-key", "device-1");

		CustomException ex = assertThrows(CustomException.class, () -> service.sendOtp("+919876543210", "123456"));
		assertEquals(HttpStatus.BAD_GATEWAY, ex.getHttpStatus());
		assertEquals("Failed to send SMS", ex.getMessage());
	}

	@Test
	void sendOtp_missingApiKey_doesNotCallGateway() throws Exception {
		SmsGatewayService service = newService("", "device-1");

		CustomException ex = assertThrows(CustomException.class, () -> service.sendOtp("+919876543210", "123456"));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getHttpStatus());
		assertEquals(0, server.getRequestCount());
	}

	private SmsGatewayService newService(String apiKey, String deviceId) {
		String base = server.url("/").toString().replaceAll("/$", "");
		return new SmsGatewayService(RestClient.builder().build(), base, apiKey, deviceId);
	}
}
