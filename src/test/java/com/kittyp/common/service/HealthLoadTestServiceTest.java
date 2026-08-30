package com.kittyp.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.kittyp.common.health.BackgroundWorkersHealthIndicator;
import com.kittyp.common.health.HealthActionPlanner;
import com.kittyp.common.model.HealthOptimizeTarget;
import com.kittyp.common.model.SystemHealthResponse.Component;
import com.kittyp.common.model.SystemHealthResponse.HealthAction;

class HealthLoadTestServiceTest {

	@TempDir
	Path temp;

	@Test
	void diskStartMakesReclaimableFilesAndStopDeletesThem() throws Exception {
		ThreadPoolTaskExecutor executor = executor(1, 4);
		DataSource dataSource = mock(DataSource.class);
		HealthLoadTestService service = new HealthLoadTestService(dataSource, executor, temp);
		try {
			service.start(HealthOptimizeTarget.DISK);
			Path file = temp.resolve(".tmp").resolve(HealthLoadTestService.DISK_DIR_NAME).resolve("load-0.bin");
			assertTrue(Files.exists(file));
			assertTrue(service.activeTargets().contains("DISK"));

			service.stop(HealthOptimizeTarget.DISK);
			assertFalse(Files.exists(file));
			assertTrue(service.activeTargets().isEmpty());
		} finally {
			service.shutdown();
			executor.shutdown();
		}
	}

	@Test
	void workersFillThenStopRecoversQueue() throws Exception {
		ThreadPoolTaskExecutor executor = executor(2, 10);
		DataSource dataSource = mock(DataSource.class);
		HealthLoadTestService service = new HealthLoadTestService(dataSource, executor, temp);
		BackgroundWorkersHealthIndicator indicator = new BackgroundWorkersHealthIndicator(executor);
		try {
			service.start(HealthOptimizeTarget.WORKERS);
			waitUntil(() -> ((Number) indicator.health().getDetails().get("queueSize")).intValue() >= 9, 2000);
			var health = indicator.health();
			assertTrue(((Number) health.getDetails().get("queueSize")).intValue() >= 9);
			HealthAction action = workersAction(health.getStatus().getCode(), health.getDetails());
			assertEquals("CRITICAL", action.severity());
			assertFalse(action.optimizeEnabled());

			service.stop(HealthOptimizeTarget.WORKERS);
			waitUntil(() -> ((Number) indicator.health().getDetails().get("queueSize")).intValue() == 0, 4000);
			var after = indicator.health();
			assertEquals(Status.UP, after.getStatus());
			assertEquals(0, ((Number) after.getDetails().get("queueSize")).intValue());
			assertEquals("OK", workersAction(after.getStatus().getCode(), after.getDetails()).severity());
		} finally {
			service.shutdown();
			executor.shutdown();
		}
	}

	@Test
	void poolStartHoldsConnectionsAndStopClosesThem() throws Exception {
		ThreadPoolTaskExecutor executor = executor(1, 1);
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(connection);
		HealthLoadTestService service = new HealthLoadTestService(dataSource, executor, temp);
		try {
			service.start(HealthOptimizeTarget.POOL);
			assertTrue(service.activeTargets().contains("POOL"));
			service.stop(HealthOptimizeTarget.POOL);
			verify(connection, atLeast(10)).close();
		} finally {
			service.shutdown();
			executor.shutdown();
		}
	}

	private static HealthAction workersAction(String status, java.util.Map<String, Object> details) {
		return HealthActionPlanner.plan(java.util.Map.of("backgroundWorkers", new Component(status, details)), 0)
				.stream().filter((row) -> "WORKERS".equals(row.target())).findFirst().orElseThrow();
	}

	private static ThreadPoolTaskExecutor executor(int maxPool, int queueCapacity) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(maxPool);
		executor.setMaxPoolSize(maxPool);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("Async-Thread-");
		executor.initialize();
		return executor;
	}

	private static void waitUntil(Check check, long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (check.ok()) {
				return;
			}
			TimeUnit.MILLISECONDS.sleep(20);
		}
		assertTrue(check.ok());
	}

	@FunctionalInterface
	private interface Check {
		boolean ok();
	}
}
