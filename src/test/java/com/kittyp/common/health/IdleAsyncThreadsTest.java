package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class IdleAsyncThreadsTest {

	@Test
	void interruptsOnlyAsyncPrefixWaitingThreads() throws Exception {
		Object lock = new Object();
		CountDownLatch parked = new CountDownLatch(1);
		AtomicBoolean interrupted = new AtomicBoolean(false);
		Thread async = new Thread(() -> {
			synchronized (lock) {
				parked.countDown();
				try {
					lock.wait();
				} catch (InterruptedException ex) {
					interrupted.set(true);
				}
			}
		}, "Async-Thread-test");
		Thread http = new Thread(() -> {
			synchronized (lock) {
				try {
					lock.wait();
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
				}
			}
		}, "http-nio-8002-exec-1");
		async.setDaemon(true);
		http.setDaemon(true);
		async.start();
		http.start();
		assertTrue(parked.await(2, TimeUnit.SECONDS));
		waitUntil(async, Thread.State.WAITING, Thread.State.TIMED_WAITING);

		int count = IdleAsyncThreads.interruptIdle(List.of(async, http));
		async.join(2000);
		http.interrupt();
		http.join(2000);

		assertEquals(1, count);
		assertTrue(interrupted.get());
	}

	@Test
	void skipsHttpNioEvenWithAsyncInName() {
		Thread fake = new Thread(() -> {
		}, "Async-Thread-http-nio-x");
		assertEquals(0, IdleAsyncThreads.interruptIdle(List.of(fake)));
	}

	private static void waitUntil(Thread thread, Thread.State... allowed) throws InterruptedException {
		for (int i = 0; i < 50; i++) {
			for (Thread.State state : allowed) {
				if (thread.getState() == state) {
					return;
				}
			}
			Thread.sleep(20);
		}
	}
}
