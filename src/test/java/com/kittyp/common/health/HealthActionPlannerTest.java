package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.kittyp.common.model.SystemHealthResponse.Component;
import com.kittyp.common.model.SystemHealthResponse.HealthAction;

class HealthActionPlannerTest {

	@Test
	void diskNinetyPercentWithNoReclaimableDisablesOptimize() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("diskSpace", new Component("UP", Map.of("total", 1000L, "free", 90L)));

		HealthAction disk = action(components, 0, "DISK");

		assertEquals("CRITICAL", disk.severity());
		assertFalse(disk.optimizeEnabled());
		assertTrue(disk.headline().contains("host"));
	}

	@Test
	void diskNinetyPercentWithReclaimableEnablesOptimize() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("diskSpace", new Component("UP", Map.of("total", 1000L, "free", 90L)));

		HealthAction disk = action(components, 2048, "DISK");

		assertEquals("CRITICAL", disk.severity());
		assertTrue(disk.optimizeEnabled());
	}

	@Test
	void memoryHealthyBelowWatch() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("memory", new Component("UP", Map.of("used", 13L, "max", 100L, "heap", 20L)));

		HealthAction memory = action(components, 0, "MEMORY");

		assertEquals("OK", memory.severity());
		assertFalse(memory.optimizeEnabled());
	}

	@Test
	void poolWaitingWithIdleCanOptimize() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("hikariPool",
				new Component("UP", Map.of("active", 2, "idle", 8, "waiting", 3, "total", 10, "max", 10)));

		HealthAction pool = action(components, 0, "POOL");

		assertEquals("CRITICAL", pool.severity());
		assertTrue(pool.optimizeEnabled());
	}

	@Test
	void poolWaitingWithoutIdleDisablesOptimize() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("hikariPool",
				new Component("UP", Map.of("active", 10, "idle", 0, "waiting", 3, "total", 10, "max", 10)));

		HealthAction pool = action(components, 0, "POOL");

		assertEquals("CRITICAL", pool.severity());
		assertFalse(pool.optimizeEnabled());
		assertTrue(pool.optimizeHint().toLowerCase().contains("active"));
	}

	@Test
	void waitingWithSpareCapacityIsCriticalNotDownOverlay() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("hikariPool",
				new Component("UP", Map.of("active", 4, "idle", 2, "waiting", 1, "total", 6, "max", 10)));

		HealthAction pool = action(components, 0, "POOL");

		assertEquals("CRITICAL", pool.severity());
		assertTrue(pool.optimizeEnabled());
	}

	@Test
	void workersLiveQueueWithoutCancelledDisablesOptimize() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("backgroundWorkers", new Component("DOWN",
				Map.of("queueSize", 9, "queueCapacity", 10, "cancelled", 0, "active", 2, "maxPoolSize", 2)));

		HealthAction workers = action(components, 0, "WORKERS");

		assertEquals("CRITICAL", workers.severity());
		assertFalse(workers.optimizeEnabled());
	}

	@Test
	void workersCancelledJobsEnableOptimize() {
		Map<String, Component> components = new LinkedHashMap<>();
		components.put("backgroundWorkers", new Component("UP",
				Map.of("queueSize", 9, "queueCapacity", 10, "cancelled", 2, "active", 2, "maxPoolSize", 2)));

		HealthAction workers = action(components, 0, "WORKERS");

		assertEquals("CRITICAL", workers.severity());
		assertTrue(workers.optimizeEnabled());
	}

	private static HealthAction action(Map<String, Component> components, long reclaimable, String target) {
		List<HealthAction> actions = HealthActionPlanner.plan(components, reclaimable);
		return actions.stream().filter((row) -> target.equals(row.target())).findFirst().orElseThrow();
	}
}
