package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.datasource.AbstractDataSource;

import com.zaxxer.hikari.HikariPoolMXBean;

class HikariPoolHealthIndicatorTest {

	@Test
	void upWhenPoolHasSpareCapacity() {
		Health health = HikariPoolHealthIndicator.fromPool(new FakePool(3, 2, 0, 5), 10);

		assertEquals(Status.UP, health.getStatus());
		assertEquals(3, health.getDetails().get("active"));
		assertEquals(2, health.getDetails().get("idle"));
		assertEquals(0, health.getDetails().get("waiting"));
		assertEquals(5, health.getDetails().get("total"));
		assertEquals(10, health.getDetails().get("max"));
	}

	@Test
	void downWhenSaturatedAndThreadsWaiting() {
		Health health = HikariPoolHealthIndicator.fromPool(new FakePool(10, 0, 2, 10), 10);

		assertEquals(Status.DOWN, health.getStatus());
	}

	@Test
	void upAfterWaitersClear() {
		assertEquals(Status.DOWN, HikariPoolHealthIndicator.fromPool(new FakePool(10, 0, 2, 10), 10).getStatus());
		assertEquals(Status.UP, HikariPoolHealthIndicator.fromPool(new FakePool(0, 10, 0, 10), 10).getStatus());
	}

	@Test
	void unknownWhenNotHikari() {
		Health health = HikariPoolHealthIndicator.inspect(new AbstractDataSource() {
			@Override
			public Connection getConnection() {
				return null;
			}

			@Override
			public Connection getConnection(String username, String password) {
				return null;
			}
		});

		assertEquals(Status.UNKNOWN, health.getStatus());
		assertEquals("not HikariDataSource", health.getDetails().get("reason"));
	}

	@Test
	void unknownWhenPoolNotStarted() {
		Health health = HikariPoolHealthIndicator.fromPool(null, 10);

		assertEquals(Status.UNKNOWN, health.getStatus());
		assertEquals("pool not started", health.getDetails().get("reason"));
	}

	private static final class FakePool implements HikariPoolMXBean {
		private final int active;
		private final int idle;
		private final int waiting;
		private final int total;

		FakePool(int active, int idle, int waiting, int total) {
			this.active = active;
			this.idle = idle;
			this.waiting = waiting;
			this.total = total;
		}

		@Override
		public int getIdleConnections() {
			return idle;
		}

		@Override
		public int getActiveConnections() {
			return active;
		}

		@Override
		public int getTotalConnections() {
			return total;
		}

		@Override
		public int getThreadsAwaitingConnection() {
			return waiting;
		}

		@Override
		public void softEvictConnections() {
		}

		@Override
		public void suspendPool() {
		}

		@Override
		public void resumePool() {
		}
	}
}
