package com.kittyp.common.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KittyPDiskCleanupTest {

	@TempDir
	Path temp;

	@Test
	void isUnderRootRejectsParentTraversal() {
		Path root = temp.resolve("safe");
		assertFalse(KittyPDiskCleanup.isUnderRoot(root.resolve("../secret.txt"), root));
		assertFalse(KittyPDiskCleanup.isUnderRoot(temp.resolve("outside.log"), root));
	}

	@Test
	void isUnderRootAllowsNestedFile() {
		Path root = temp.resolve("safe");
		assertTrue(KittyPDiskCleanup.isUnderRoot(root.resolve("logs").resolve("app.log"), root));
	}

	@Test
	void deleteOnlyOldFilesInsideAllowlist() throws Exception {
		Path logs = temp.resolve("logs");
		Files.createDirectories(logs);
		Path oldFile = logs.resolve("old.log");
		Files.writeString(oldFile, "abcdefghij");
		Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(Duration.ofHours(2))));
		Path outside = temp.resolve("outside.log");
		Files.writeString(outside, "do-not-delete");
		Files.setLastModifiedTime(outside, FileTime.from(Instant.now().minus(Duration.ofHours(2))));

		KittyPDiskCleanup cleanup = new KittyPDiskCleanup("", 1024 * 1024, Duration.ofMinutes(10), temp, temp);
		long deleted = cleanup.deleteReclaimable();

		assertTrue(deleted >= 10);
		assertFalse(Files.exists(oldFile));
		assertTrue(Files.exists(outside));
	}

	@Test
	void skipsFreshFiles() throws Exception {
		Path tmpKittyp = temp.resolve(".tmp");
		Files.createDirectories(tmpKittyp);
		Path fresh = tmpKittyp.resolve("fresh.log");
		Files.writeString(fresh, "new");

		KittyPDiskCleanup cleanup = new KittyPDiskCleanup("", 1024 * 1024, Duration.ofMinutes(10), temp, temp);
		assertEquals(0, cleanup.deleteReclaimable());
		assertTrue(Files.exists(fresh));
	}

	@Test
	void deletesAgedLoadTestFilesUnderTmp() throws Exception {
		Path dir = temp.resolve(".tmp").resolve("kittyp-health-load");
		Files.createDirectories(dir);
		Path oldFile = dir.resolve("load-0.bin");
		Files.write(oldFile, new byte[32]);
		Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(Duration.ofMinutes(11))));

		KittyPDiskCleanup cleanup = new KittyPDiskCleanup("", 1024 * 1024, Duration.ofMinutes(10), temp, temp);
		assertTrue(cleanup.deleteReclaimable() >= 32);
		assertFalse(Files.exists(oldFile));
	}
}
