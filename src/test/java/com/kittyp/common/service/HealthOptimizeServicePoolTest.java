package com.kittyp.common.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.kittyp.common.health.KittyPDiskCleanup;
import com.kittyp.common.model.HealthOptimizeResponse;
import com.kittyp.common.model.HealthOptimizeTarget;
import com.kittyp.common.model.SystemHealthResponse;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

class HealthOptimizeServicePoolTest {

	@Test
	void poolOptimizeSoftEvictsAndNeverCloses() {
		HikariPoolMXBean pool = mock(HikariPoolMXBean.class);
		HikariDataSource hikari = mock(HikariDataSource.class);
		when(hikari.getHikariPoolMXBean()).thenReturn(pool);
		when(pool.getThreadsAwaitingConnection()).thenReturn(1);
		when(pool.getActiveConnections()).thenReturn(2);

		SystemHealthService health = mock(SystemHealthService.class);
		when(health.snapshot()).thenReturn(new SystemHealthResponse("UP", new LinkedHashMap<>()));
		@SuppressWarnings("unchecked")
		ObjectProvider<org.springframework.cache.CacheManager> caches = mock(ObjectProvider.class);
		when(caches.getIfAvailable()).thenReturn(null);
		KittyPDiskCleanup disk = mock(KittyPDiskCleanup.class);

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1);
		executor.setThreadNamePrefix("Async-Thread-");
		executor.initialize();
		try {
			HealthOptimizeService service = new HealthOptimizeService(health, hikari, executor, caches, disk);
			HealthOptimizeResponse response = service.optimize(HealthOptimizeTarget.POOL);
			assertTrue(response.applied());
			verify(pool).softEvictConnections();
			verify(hikari, never()).close();
		} finally {
			executor.shutdown();
		}
	}

	@Test
	void memoryOptimizeDoesNotInterruptIdleAsyncOrHttp() throws Exception {
		SystemHealthService health = mock(SystemHealthService.class);
		when(health.snapshot()).thenReturn(new SystemHealthResponse("UP", new LinkedHashMap<>()));
		@SuppressWarnings("unchecked")
		ObjectProvider<org.springframework.cache.CacheManager> caches = mock(ObjectProvider.class);
		when(caches.getIfAvailable()).thenReturn(null);
		KittyPDiskCleanup disk = mock(KittyPDiskCleanup.class);
		HikariDataSource hikari = mock(HikariDataSource.class);

		Object lock = new Object();
		CountDownLatch parked = new CountDownLatch(2);
		AtomicBoolean asyncInterrupted = new AtomicBoolean(false);
		AtomicBoolean httpInterrupted = new AtomicBoolean(false);
		Thread async = park(lock, parked, asyncInterrupted, "Async-Thread-test");
		Thread http = park(lock, parked, httpInterrupted, "http-nio-8002-exec-1");
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1);
		executor.setThreadNamePrefix("Async-Thread-");
		executor.initialize();
		try {
			assertTrue(parked.await(2, TimeUnit.SECONDS));
			HealthOptimizeService service = new HealthOptimizeService(health, hikari, executor, caches, disk);
			HealthOptimizeResponse response = service.optimize(HealthOptimizeTarget.MEMORY);
			assertFalse(response.applied());
			TimeUnit.MILLISECONDS.sleep(150);
			assertFalse(asyncInterrupted.get());
			assertFalse(httpInterrupted.get());
		} finally {
			async.interrupt();
			http.interrupt();
			async.join(1000);
			http.join(1000);
			executor.shutdown();
		}
	}

	private static Thread park(Object lock, CountDownLatch parked, AtomicBoolean interrupted, String name) {
		Thread thread = new Thread(() -> {
			synchronized (lock) {
				parked.countDown();
				try {
					lock.wait();
				} catch (InterruptedException ex) {
					interrupted.set(true);
				}
			}
		}, name);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}
}
