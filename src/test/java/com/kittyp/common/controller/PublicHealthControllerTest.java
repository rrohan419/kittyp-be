package com.kittyp.common.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PublicHealthControllerTest {

	@Test
	void upReturnsStatusOnly() {
		HealthIndicator hikari = () -> Health.up().withDetail("active", 3).build();
		HealthIndicator memory = () -> Health.up().withDetail("used", 99).build();
		PublicHealthController controller = new PublicHealthController(List.of(hikari, memory));

		ResponseEntity<Map<String, String>> response = controller.health();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		Map<String, String> body = response.getBody();
		assertEquals("UP", body.get("status"));
		assertEquals(1, body.size());
		assertFalse(body.containsKey("hikariPool"));
		assertFalse(body.containsKey("memory"));
		assertFalse(body.containsKey("jvm"));
	}

	@Test
	void downReturns503WithoutDetails() {
		HealthIndicator db = () -> Health.down().withDetail("error", "connection refused").build();
		PublicHealthController controller = new PublicHealthController(List.of(db));

		ResponseEntity<Map<String, String>> response = controller.health();

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
		Map<String, String> body = response.getBody();
		assertEquals("DOWN", body.get("status"));
		assertEquals(1, body.size());
		assertFalse(body.containsKey("error"));
		assertFalse(body.containsKey("db"));
	}

	@Test
	void emptyRegistryIsUp() {
		PublicHealthController controller = new PublicHealthController(List.of());

		ResponseEntity<Map<String, String>> response = controller.health();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(Map.of("status", "UP"), response.getBody());
	}
}
