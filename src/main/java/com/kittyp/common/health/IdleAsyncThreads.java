package com.kittyp.common.health;

import java.lang.Thread.State;
import java.util.Collection;

/**
 * Interrupt idle KittyP async workers only. Never Tomcat request threads.
 */
public final class IdleAsyncThreads {

	public static final String PREFIX = "Async-Thread-";

	private IdleAsyncThreads() {
	}

	public static int interruptIdle(Collection<Thread> threads) {
		int interrupted = 0;
		for (Thread thread : threads) {
			if (thread == null || thread.getName() == null) {
				continue;
			}
			String name = thread.getName();
			if (!name.startsWith(PREFIX) || name.contains("http-nio")) {
				continue;
			}
			State state = thread.getState();
			if (state != State.WAITING && state != State.TIMED_WAITING) {
				continue;
			}
			thread.interrupt();
			interrupted++;
		}
		return interrupted;
	}
}
