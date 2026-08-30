package com.kittyp.common.health;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

final class CancelledWorkerJobs {

	private CancelledWorkerJobs() {
	}

	static int count(ThreadPoolExecutor executor) {
		if (executor == null) {
			return 0;
		}
		int cancelled = 0;
		for (Runnable task : executor.getQueue()) {
			if (task instanceof Future<?> future && future.isCancelled()) {
				cancelled++;
			}
		}
		return cancelled;
	}
}
