package com.kittyp.common.service;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.health.HealthActionPlanner;
import com.kittyp.common.health.KittyPDiskCleanup;
import com.kittyp.common.model.HealthOptimizeResponse;
import com.kittyp.common.model.HealthOptimizeTarget;
import com.kittyp.common.model.SystemHealthResponse;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HealthOptimizeService {

	static final Duration COOLDOWN = Duration.ofSeconds(30);

	private final SystemHealthService systemHealthService;
	private final DataSource dataSource;
	private final ThreadPoolTaskExecutor taskExecutor;
	private final ObjectProvider<CacheManager> cacheManager;
	private final KittyPDiskCleanup diskCleanup;
	private final Map<HealthOptimizeTarget, Instant> lastRun = new ConcurrentHashMap<>();

	public HealthOptimizeService(SystemHealthService systemHealthService, DataSource dataSource,
			@Qualifier("taskExecutor") TaskExecutor taskExecutor, ObjectProvider<CacheManager> cacheManager,
			KittyPDiskCleanup diskCleanup) {
		if (!(taskExecutor instanceof ThreadPoolTaskExecutor executor)) {
			throw new IllegalStateException("taskExecutor is not ThreadPoolTaskExecutor");
		}
		this.systemHealthService = systemHealthService;
		this.dataSource = dataSource;
		this.taskExecutor = executor;
		this.cacheManager = cacheManager;
		this.diskCleanup = diskCleanup;
	}

	public HealthOptimizeResponse optimize(HealthOptimizeTarget target) {
		if (target == null) {
			throw new CustomException("Optimize target is required", HttpStatus.BAD_REQUEST);
		}
		Instant now = Instant.now();
		Instant previous = lastRun.get(target);
		if (previous != null && Duration.between(previous, now).compareTo(COOLDOWN) < 0) {
			long wait = COOLDOWN.minus(Duration.between(previous, now)).toSeconds();
			throw new CustomException("Wait " + Math.max(1, wait) + "s before optimizing " + target + " again",
					HttpStatus.TOO_MANY_REQUESTS);
		}
		SystemHealthResponse before = systemHealthService.snapshot();
		Result result = apply(target);
		lastRun.put(target, Instant.now());
		SystemHealthResponse after = systemHealthService.snapshot();
		log.info("Health optimize target={} applied={} summary={}", target, result.applied, result.summary);
		return new HealthOptimizeResponse(target.name(), result.applied, result.summary, result.remainingHint, before,
				after);
	}

	private Result apply(HealthOptimizeTarget target) {
		return switch (target) {
		case MEMORY -> optimizeMemory();
		case DISK -> optimizeDisk();
		case POOL -> optimizePool();
		case WORKERS -> optimizeWorkers();
		};
	}

	private Result optimizeMemory() {
		int caches = clearCaches();
		int purged = purgeCancelled();
		System.gc();
		boolean applied = caches > 0 || purged > 0;
		if (!applied) {
			return new Result(false,
					"No caches or cancelled jobs to clear. Requested GC as a hint. Heap stays high if the app still holds objects.",
					"Stop induced load or restart the KittyP backend if heap stays critical. Postgres is untouched.");
		}
		return new Result(true,
				"Cleared " + caches + " cache(s) and purged " + purged + " cancelled job(s), then requested GC.",
				"GC will not drop heap if the app still holds objects. Postgres is untouched.");
	}

	private Result optimizeDisk() {
		long bytes = diskCleanup.deleteReclaimable();
		if (bytes <= 0) {
			return new Result(false,
					"No KittyP temp or log files were old enough to delete. This volume is the host disk, not the JVM heap.",
					"Free space in Finder or move the database. Optimize cannot wipe the disk or Postgres data.");
		}
		return new Result(true, "Deleted about " + HealthActionPlanner.formatBytes(bytes) + " of KittyP temp/log files.",
				"Host disk usage may barely change if other apps fill the volume.");
	}

	private Result optimizePool() {
		HikariDataSource hikari = unwrapHikari();
		if (hikari == null) {
			return new Result(false, "Database pool is not HikariCP, so idle connections cannot be evicted here.",
					"Check Postgres on localhost:5433 if the DB component is DOWN.");
		}
		HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
		if (pool == null) {
			return new Result(false, "Connection pool is not started yet.",
					"Wait for the app to finish starting, then retry.");
		}
		int waiting = pool.getThreadsAwaitingConnection();
		int active = pool.getActiveConnections();
		pool.softEvictConnections();
		String extra = waiting > 0 && active > 0
				? " " + active + " active query connection(s) were left running."
				: "";
		return new Result(true, "Asked Hikari to drop idle connections." + extra,
				"Active queries were not aborted. If waiters remain, finish or time out those queries.");
	}

	private Result optimizeWorkers() {
		int purged = purgeCancelled();
		if (purged <= 0) {
			return new Result(false, "No cancelled jobs to clear.",
					"Running HTTP and in-flight async work was left alone. Stop load or wait for live jobs to finish.");
		}
		return new Result(true, "Purged " + purged + " cancelled job(s).",
				"Tomcat request threads and live async work were not interrupted.");
	}

	private int purgeCancelled() {
		try {
			var executor = taskExecutor.getThreadPoolExecutor();
			int before = executor.getQueue().size();
			executor.purge();
			return Math.max(0, before - executor.getQueue().size());
		} catch (RuntimeException ex) {
			log.warn("Could not purge worker queue: {}", ex.getMessage());
			return 0;
		}
	}

	private int clearCaches() {
		CacheManager manager = cacheManager.getIfAvailable();
		if (manager == null) {
			return 0;
		}
		int count = 0;
		for (String name : manager.getCacheNames()) {
			Cache cache = manager.getCache(name);
			if (cache != null) {
				cache.clear();
				count++;
			}
		}
		return count;
	}

	private HikariDataSource unwrapHikari() {
		try {
			if (dataSource instanceof HikariDataSource hikari) {
				return hikari;
			}
			if (dataSource.isWrapperFor(HikariDataSource.class)) {
				return dataSource.unwrap(HikariDataSource.class);
			}
		} catch (SQLException ex) {
			log.warn("Could not unwrap Hikari pool: {}", ex.getMessage());
		}
		return null;
	}

	private record Result(boolean applied, String summary, String remainingHint) {
	}
}
