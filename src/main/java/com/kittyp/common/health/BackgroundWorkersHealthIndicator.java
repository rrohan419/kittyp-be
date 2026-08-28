package com.kittyp.common.health;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class BackgroundWorkersHealthIndicator implements HealthIndicator {

	private final ThreadPoolTaskExecutor taskExecutor;

	public BackgroundWorkersHealthIndicator(@Qualifier("taskExecutor") TaskExecutor taskExecutor) {
		if (!(taskExecutor instanceof ThreadPoolTaskExecutor executor)) {
			throw new IllegalStateException("taskExecutor is not ThreadPoolTaskExecutor");
		}
		this.taskExecutor = executor;
	}

	@Override
	public Health health() {
		int active = taskExecutor.getActiveCount();
		int poolSize = taskExecutor.getPoolSize();
		int maxPoolSize = taskExecutor.getMaxPoolSize();
		int queueCapacity = taskExecutor.getQueueCapacity();
		int queueSize = taskExecutor.getThreadPoolExecutor().getQueue().size();
		boolean queueFull = queueCapacity > 0 && queueSize >= queueCapacity;
		Health.Builder builder = queueFull ? Health.down() : Health.up();
		return builder
				.withDetail("active", active)
				.withDetail("poolSize", poolSize)
				.withDetail("maxPoolSize", maxPoolSize)
				.withDetail("queueSize", queueSize)
				.withDetail("queueCapacity", queueCapacity)
				.build();
	}
}
