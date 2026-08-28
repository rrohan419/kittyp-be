package com.kittyp.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import com.kittyp.common.model.SystemHealthResponse;

class SystemHealthServiceTest {

	@Test
	void assembleMapsEveryComponentAndOverallStatus() {
		Map<String, Health> healths = new LinkedHashMap<>();
		healths.put("memory", Health.up().withDetail("used", 50L).withDetail("max", 100L).build());
		healths.put("diskSpace", Health.up().withDetail("total", 1000L).withDetail("free", 400L).build());
		healths.put("ping", Health.up().build());
		healths.put("ssl", Health.up().withDetail("validChains", 1).withDetail("invalidChains", 0).build());
		healths.put("livenessState", Health.up().build());
		healths.put("readinessState", Health.up().build());

		SystemHealthResponse response = SystemHealthService.assemble(healths);

		assertEquals("UP", response.status());
		assertEquals(6, response.components().size());
		assertEquals(50L, ((Number) response.components().get("memory").details().get("used")).longValue());
		assertEquals(400L, ((Number) response.components().get("diskSpace").details().get("free")).longValue());
		assertEquals("UP", response.components().get("ping").status());
		assertEquals(1, ((Number) response.components().get("ssl").details().get("validChains")).intValue());
		assertTrue(response.components().get("livenessState").details().isEmpty());
	}

	@Test
	void assembleIsDownWhenAnyComponentIsDown() {
		Map<String, Health> healths = new LinkedHashMap<>();
		healths.put("memory", Health.down().withDetail("used", 99L).withDetail("max", 100L).build());
		healths.put("ping", Health.up().build());

		SystemHealthResponse response = SystemHealthService.assemble(healths);

		assertEquals("DOWN", response.status());
		assertEquals("DOWN", response.components().get("memory").status());
	}

	@Test
	void contributorNameStripsHealthIndicatorSuffix() {
		assertEquals("memory", SystemHealthService.contributorName("memoryHealthIndicator"));
		assertEquals("hikariPool", SystemHealthService.contributorName("hikariPoolHealthIndicator"));
		assertEquals("diskSpace", SystemHealthService.contributorName("diskSpaceHealthIndicator"));
		assertEquals("db", SystemHealthService.contributorName("dbHealthContributor"));
	}
}
