package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class BackgroundWorkersHealthIndicatorTest {

	@Test
	void upWhenQueueHasCapacity() {
		ThreadPoolTaskExecutor executor = executor(2, 8);
		try {
			Health health = new BackgroundWorkersHealthIndicator(executor).health();

			assertEquals(Status.UP, health.getStatus());
			assertEquals(0, health.getDetails().get("active"));
			assertEquals(2, health.getDetails().get("maxPoolSize"));
			assertEquals(0, health.getDetails().get("queueSize"));
			assertEquals(8, health.getDetails().get("queueCapacity"));
		} finally {
			executor.shutdown();
		}
	}

	@Test
	void downWhenQueueIsFull() throws Exception {
		ThreadPoolTaskExecutor executor = executor(1, 1);
		CountDownLatch hold = new CountDownLatch(1);
		CountDownLatch started = new CountDownLatch(1);
		try {
			executor.execute(() -> {
				started.countDown();
				await(hold);
			});
			assertTrue(started.await(2, TimeUnit.SECONDS));
			executor.execute(() -> {
			});

			Health health = new BackgroundWorkersHealthIndicator(executor).health();

			assertEquals(Status.DOWN, health.getStatus());
			assertEquals(1, health.getDetails().get("queueSize"));
			assertEquals(1, health.getDetails().get("active"));
		} finally {
			hold.countDown();
			executor.shutdown();
		}
	}

	@Test
	void rejectsNonThreadPoolExecutor() {
		assertThrows(IllegalStateException.class,
				() -> new BackgroundWorkersHealthIndicator(new SyncTaskExecutor()));
	}

	private static ThreadPoolTaskExecutor executor(int maxPool, int queueCapacity) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(maxPool);
		executor.setMaxPoolSize(maxPool);
		executor.setQueueCapacity(queueCapacity);
		executor.initialize();
		return executor;
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
