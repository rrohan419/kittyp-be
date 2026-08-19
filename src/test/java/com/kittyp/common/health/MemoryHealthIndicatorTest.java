package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class MemoryHealthIndicatorTest {

	@Test
	void upWhenBelowThreshold() {
		MemoryHealthIndicator indicator = new MemoryHealthIndicator(
				() -> new MemoryHealthIndicator.MemoryStats(50, 100, 80));

		Health health = indicator.health();

		assertEquals(Status.UP, health.getStatus());
		assertEquals(50L, health.getDetails().get("used"));
		assertEquals(100L, health.getDetails().get("max"));
		assertEquals(80L, health.getDetails().get("heap"));
	}

	@Test
	void downWhenAtOrAboveThreshold() {
		MemoryHealthIndicator indicator = new MemoryHealthIndicator(
				() -> new MemoryHealthIndicator.MemoryStats(95, 100, 100));

		Health health = indicator.health();

		assertEquals(Status.DOWN, health.getStatus());
		assertTrue(health.getDetails().containsKey("used"));
		assertTrue(health.getDetails().containsKey("max"));
		assertTrue(health.getDetails().containsKey("heap"));
	}
}
