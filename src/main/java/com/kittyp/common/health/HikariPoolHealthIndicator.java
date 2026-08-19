package com.kittyp.common.health;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@Component
public class HikariPoolHealthIndicator implements HealthIndicator {

	private final DataSource dataSource;

	public HikariPoolHealthIndicator(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Health health() {
		return inspect(dataSource);
	}

	static Health inspect(DataSource dataSource) {
		HikariDataSource hikari;
		try {
			hikari = unwrap(dataSource);
		} catch (SQLException ex) {
			return Health.unknown().withDetail("reason", "unwrap failed").build();
		}
		if (hikari == null) {
			return Health.unknown().withDetail("reason", "not HikariDataSource").build();
		}
		return fromPool(hikari.getHikariPoolMXBean(), hikari.getMaximumPoolSize());
	}

	static Health fromPool(HikariPoolMXBean pool, int max) {
		if (pool == null) {
			return Health.unknown().withDetail("reason", "pool not started").build();
		}
		int active = pool.getActiveConnections();
		int idle = pool.getIdleConnections();
		int waiting = pool.getThreadsAwaitingConnection();
		int total = pool.getTotalConnections();
		boolean saturated = waiting > 0 && active >= max;
		Health.Builder builder = saturated ? Health.down() : Health.up();
		return builder
				.withDetail("active", active)
				.withDetail("idle", idle)
				.withDetail("waiting", waiting)
				.withDetail("total", total)
				.withDetail("max", max)
				.build();
	}

	private static HikariDataSource unwrap(DataSource dataSource) throws SQLException {
		if (dataSource instanceof HikariDataSource hikari) {
			return hikari;
		}
		if (dataSource.isWrapperFor(HikariDataSource.class)) {
			return dataSource.unwrap(HikariDataSource.class);
		}
		return null;
	}
}
