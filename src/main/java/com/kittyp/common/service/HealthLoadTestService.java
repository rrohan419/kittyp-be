package com.kittyp.common.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.HealthOptimizeTarget;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "kittyp.health.load-test.enabled", havingValue = "true")
public class HealthLoadTestService {

	static final Duration AUTO_STOP = Duration.ofSeconds(60);
	static final String DISK_DIR_NAME = "kittyp-health-load";
	static final long MEMORY_CAP_BYTES = 64L * 1024 * 1024;
	static final int DISK_FILE_COUNT = 4;
	static final int DISK_FILE_BYTES = 256 * 1024;

	private final DataSource dataSource;
	private final ThreadPoolTaskExecutor taskExecutor;
	private final Path userDir;
	private final Map<HealthOptimizeTarget, Session> sessions = new EnumMap<>(HealthOptimizeTarget.class);
	private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(thread -> {
		Thread worker = new Thread(thread, "health-load-watchdog");
		worker.setDaemon(true);
		return worker;
	});
	private final ExecutorService holders = Executors.newCachedThreadPool(thread -> {
		Thread worker = new Thread(thread, "health-load-holder");
		worker.setDaemon(true);
		return worker;
	});

	@Autowired
	public HealthLoadTestService(DataSource dataSource, @Qualifier("taskExecutor") TaskExecutor taskExecutor) {
		this(dataSource, taskExecutor, Path.of(System.getProperty("user.dir", ".")));
	}

	HealthLoadTestService(DataSource dataSource, TaskExecutor taskExecutor, Path userDir) {
		if (!(taskExecutor instanceof ThreadPoolTaskExecutor executor)) {
			throw new IllegalStateException("taskExecutor is not ThreadPoolTaskExecutor");
		}
		this.dataSource = dataSource;
		this.taskExecutor = executor;
		this.userDir = userDir;
	}

	public synchronized List<String> activeTargets() {
		return sessions.keySet().stream().map(Enum::name).toList();
	}

	public synchronized String start(HealthOptimizeTarget target) {
		if (target == null) {
			throw new CustomException("Load target is required", HttpStatus.BAD_REQUEST);
		}
		if (sessions.containsKey(target)) {
			throw new CustomException(target + " load is already running. Stop it first.", HttpStatus.CONFLICT);
		}
		Session session = new Session();
		try {
			switch (target) {
			case MEMORY -> startMemory(session);
			case DISK -> startDisk(session);
			case POOL -> startPool(session);
			case WORKERS -> startWorkers(session);
			}
		} catch (RuntimeException | IOException | InterruptedException | SQLException ex) {
			session.release();
			if (ex instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new CustomException("Could not start " + target + " load: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
		}
		session.autoStop = watchdog.schedule(() -> {
			try {
				stop(target);
			} catch (RuntimeException ex) {
				log.warn("Auto-stop {} load failed: {}", target, ex.getMessage());
			}
		}, AUTO_STOP.toSeconds(), TimeUnit.SECONDS);
		sessions.put(target, session);
		log.info("Health load test started target={}", target);
		return switch (target) {
		case MEMORY -> "Retaining a capped heap buffer. Stop load (or wait 60s) to release it. Optimize GC will not free it.";
		case DISK -> "Wrote aged KittyP temp files under .tmp/" + DISK_DIR_NAME + ". Optimize can delete them, or Stop load.";
		case POOL -> "Holding the database pool. Waiting threads are a real stall. Stop load to release connections.";
		case WORKERS -> "Parked async jobs until the queue is ~90% full. Stop load to drain. Optimize cannot kill live jobs.";
		};
	}

	public synchronized String stop(HealthOptimizeTarget target) {
		if (target == null) {
			stopAll();
			return "Stopped all induced load.";
		}
		Session session = sessions.remove(target);
		if (session == null) {
			return "No induced " + target + " load was running.";
		}
		session.release();
		log.info("Health load test stopped target={}", target);
		return "Stopped " + target + " load. Metrics should fall on the next snapshot.";
	}

	public synchronized void stopAll() {
		List<HealthOptimizeTarget> targets = new ArrayList<>(sessions.keySet());
		for (HealthOptimizeTarget target : targets) {
			stop(target);
		}
	}

	@PreDestroy
	void shutdown() {
		stopAll();
		watchdog.shutdownNow();
		holders.shutdownNow();
	}

	private void startMemory(Session session) {
		long max = Runtime.getRuntime().maxMemory();
		int size = (int) Math.min(MEMORY_CAP_BYTES, Math.max(1, max / 4));
		byte[] blob = new byte[size];
		blob[0] = 1;
		blob[size - 1] = 1;
		session.memory.set(blob);
	}

	private void startDisk(Session session) throws IOException {
		Path dir = userDir.resolve(".tmp").resolve(DISK_DIR_NAME);
		Files.createDirectories(dir);
		FileTime aged = FileTime.from(Instant.now().minus(Duration.ofMinutes(11)));
		for (int i = 0; i < DISK_FILE_COUNT; i++) {
			Path file = dir.resolve("load-" + i + ".bin");
			Files.write(file, new byte[DISK_FILE_BYTES]);
			Files.setLastModifiedTime(file, aged);
		}
		session.diskDir = dir;
	}

	private void startPool(Session session) throws SQLException, InterruptedException {
		int max = poolMax();
		CountDownLatch acquired = new CountDownLatch(max);
		for (int i = 0; i < max; i++) {
			holders.execute(() -> {
				Connection connection = null;
				try {
					connection = dataSource.getConnection();
					session.connections.add(connection);
					acquired.countDown();
					session.hold.await();
				} catch (SQLException | InterruptedException ex) {
					if (ex instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					acquired.countDown();
				} finally {
					closeQuietly(connection);
				}
			});
		}
		if (!acquired.await(15, TimeUnit.SECONDS)) {
			session.hold.countDown();
			throw new CustomException("Timed out holding database connections", HttpStatus.BAD_REQUEST);
		}
		holders.execute(() -> {
			try (Connection ignored = dataSource.getConnection()) {
				session.hold.await();
			} catch (SQLException | InterruptedException ex) {
				if (ex instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
			}
		});
	}

	private void startWorkers(Session session) {
		int cap = Math.max(1, taskExecutor.getQueueCapacity());
		int queued = Math.max(1, (int) Math.ceil(cap * 0.90));
		int toSubmit = queued + Math.max(1, taskExecutor.getMaxPoolSize());
		for (int i = 0; i < toSubmit; i++) {
			Future<?> future = taskExecutor.submit(() -> awaitHold(session.hold));
			session.workerJobs.add(future);
		}
	}

	private int poolMax() {
		if (dataSource instanceof HikariDataSource hikari) {
			return Math.max(1, hikari.getMaximumPoolSize());
		}
		try {
			if (dataSource.isWrapperFor(HikariDataSource.class)) {
				return Math.max(1, dataSource.unwrap(HikariDataSource.class).getMaximumPoolSize());
			}
		} catch (SQLException ex) {
			log.debug("Could not unwrap Hikari for load test: {}", ex.getMessage());
		}
		return 10;
	}

	private static void awaitHold(CountDownLatch hold) {
		try {
			hold.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private static void closeQuietly(Connection connection) {
		if (connection == null) {
			return;
		}
		try {
			connection.close();
		} catch (SQLException ex) {
			// ignore
		}
	}

	private static void deleteTree(Path start) {
		if (start == null || !Files.exists(start)) {
			return;
		}
		try {
			Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.deleteIfExists(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.deleteIfExists(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ex) {
			// best effort
		}
	}

	private static final class Session {
		private final CountDownLatch hold = new CountDownLatch(1);
		private final AtomicReference<byte[]> memory = new AtomicReference<>();
		private final List<Connection> connections = java.util.Collections.synchronizedList(new ArrayList<>());
		private final List<Future<?>> workerJobs = new ArrayList<>();
		private Path diskDir;
		private ScheduledFuture<?> autoStop;

		private void release() {
			if (autoStop != null) {
				autoStop.cancel(false);
			}
			memory.set(null);
			hold.countDown();
			for (Connection connection : connections) {
				closeQuietly(connection);
			}
			connections.clear();
			deleteTree(diskDir);
			diskDir = null;
		}
	}
}
