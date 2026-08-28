package com.kittyp.common.health;

import java.util.function.Supplier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MemoryHealthIndicator implements HealthIndicator {

	static final double DOWN_RATIO = 0.95;

	private final Supplier<MemoryStats> stats;

	public MemoryHealthIndicator() {
		this(MemoryHealthIndicator::fromRuntime);
	}

	MemoryHealthIndicator(Supplier<MemoryStats> stats) {
		this.stats = stats;
	}

	@Override
	public Health health() {
		MemoryStats snapshot = stats.get();
		Health.Builder builder = snapshot.usedRatio() >= DOWN_RATIO ? Health.down() : Health.up();
		return builder
				.withDetail("used", snapshot.used())
				.withDetail("max", snapshot.max())
				.withDetail("heap", snapshot.heap())
				.build();
	}

	private static MemoryStats fromRuntime() {
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		return new MemoryStats(used, runtime.maxMemory(), runtime.totalMemory());
	}

	record MemoryStats(long used, long max, long heap) {
		double usedRatio() {
			return max == 0 ? 0 : (double) used / max;
		}
	}
}
