package com.kittyp.common.health;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KittyPDiskCleanup {

	static final long DEFAULT_MAX_BYTES = 256L * 1024 * 1024;

	private final Path tmpDir;
	private final Path userDir;
	private final List<Path> extraRoots;
	private final long maxBytes;
	private final Duration minAge;

	@Autowired
	public KittyPDiskCleanup(
			@Value("${kittyp.health.cleanup-dirs:}") String extraDirsCsv,
			@Value("${kittyp.health.max-cleanup-bytes:268435456}") long maxBytes,
			@Value("${kittyp.health.min-file-age-minutes:10}") long minAgeMinutes) {
		this(extraDirsCsv, maxBytes, Duration.ofMinutes(Math.max(0, minAgeMinutes)),
				Path.of(System.getProperty("java.io.tmpdir", ".")),
				Path.of(System.getProperty("user.dir", ".")));
	}

	KittyPDiskCleanup(String extraDirsCsv, long maxBytes, Duration minAge, Path tmpDir, Path userDir) {
		this.tmpDir = tmpDir;
		this.userDir = userDir;
		this.maxBytes = maxBytes > 0 ? maxBytes : DEFAULT_MAX_BYTES;
		this.minAge = minAge == null ? Duration.ofMinutes(10) : minAge;
		this.extraRoots = parseExtra(extraDirsCsv);
	}

	public long reclaimableBytes() {
		return scan(false).bytes;
	}

	public long deleteReclaimable() {
		return scan(true).bytes;
	}

	static boolean isUnderRoot(Path path, Path root) {
		if (path == null || root == null) {
			return false;
		}
		Path normalized = path.toAbsolutePath().normalize();
		Path rootNorm = root.toAbsolutePath().normalize();
		return normalized.startsWith(rootNorm) && !normalized.equals(rootNorm);
	}

	private ScanResult scan(boolean delete) {
		List<Path> roots = new ArrayList<>();
		roots.add(userDir.resolve(".tmp"));
		roots.add(userDir.resolve("logs"));
		roots.addAll(extraRoots);
		long bytes = 0;
		Instant cutoff = Instant.now().minus(minAge);
		for (Path root : roots) {
			bytes += walkRoot(root, root, cutoff, delete, this.maxBytes - bytes);
			if (bytes >= this.maxBytes) {
				break;
			}
		}
		if (bytes < this.maxBytes) {
			bytes += walkKittypTemp(cutoff, delete, this.maxBytes - bytes);
		}
		return new ScanResult(bytes);
	}

	private long walkKittypTemp(Instant cutoff, boolean delete, long remaining) {
		if (!Files.isDirectory(tmpDir)) {
			return 0;
		}
		long bytes = 0;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(tmpDir, "kittyp-*")) {
			for (Path entry : stream) {
				if (bytes >= remaining) {
					break;
				}
				if (Files.isSymbolicLink(entry)) {
					continue;
				}
				if (!isUnderRoot(entry, tmpDir) && !entry.toAbsolutePath().normalize().startsWith(tmpDir.toAbsolutePath().normalize())) {
					continue;
				}
				if (Files.isDirectory(entry)) {
					bytes += walkRoot(entry, tmpDir, cutoff, delete, remaining - bytes);
				} else {
					bytes += considerFile(entry, tmpDir, cutoff, delete, remaining - bytes);
				}
			}
		} catch (IOException ex) {
			log.debug("Could not list kittyp temp files: {}", ex.getMessage());
		}
		return bytes;
	}

	private long walkRoot(Path start, Path root, Instant cutoff, boolean delete, long remaining) {
		if (remaining <= 0 || start == null || !Files.isDirectory(start) || Files.isSymbolicLink(start)) {
			return 0;
		}
		Path startNorm = start.toAbsolutePath().normalize();
		Path rootNorm = root.toAbsolutePath().normalize();
		if (!startNorm.startsWith(rootNorm)) {
			return 0;
		}
		final long[] total = { 0 };
		try {
			Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					if (total[0] >= remaining) {
						return FileVisitResult.TERMINATE;
					}
					total[0] += considerFile(file, root, cutoff, delete, remaining - total[0]);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					if (Files.isSymbolicLink(dir) && !dir.equals(start)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					if (!dir.toAbsolutePath().normalize().startsWith(rootNorm)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException ex) {
			log.debug("Could not walk {}: {}", start, ex.getMessage());
		}
		return total[0];
	}

	private long considerFile(Path file, Path root, Instant cutoff, boolean delete, long remaining) {
		if (remaining <= 0 || Files.isSymbolicLink(file) || !isUnderRoot(file, root)) {
			return 0;
		}
		try {
			BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
			if (attrs.isDirectory() || attrs.lastModifiedTime().toInstant().isAfter(cutoff)) {
				return 0;
			}
			long size = Math.min(attrs.size(), remaining);
			if (size <= 0) {
				return 0;
			}
			if (delete) {
				Files.deleteIfExists(file);
			}
			return size;
		} catch (IOException ex) {
			log.debug("Skip {}: {}", file, ex.getMessage());
			return 0;
		}
	}

	private static List<Path> parseExtra(String csv) {
		List<Path> paths = new ArrayList<>();
		if (csv == null || csv.isBlank()) {
			return paths;
		}
		for (String part : csv.split(",")) {
			String trimmed = part.trim();
			if (!trimmed.isEmpty()) {
				paths.add(Path.of(trimmed));
			}
		}
		return paths;
	}

	private record ScanResult(long bytes) {
	}
}
