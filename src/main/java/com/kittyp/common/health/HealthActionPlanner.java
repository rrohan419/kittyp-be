package com.kittyp.common.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.kittyp.common.model.HealthOptimizeTarget;
import com.kittyp.common.model.SystemHealthResponse.Component;
import com.kittyp.common.model.SystemHealthResponse.HealthAction;

public final class HealthActionPlanner {

	static final double WATCH_RATIO = 0.70;
	static final double CRITICAL_RATIO = 0.90;

	private HealthActionPlanner() {
	}

	public static List<HealthAction> plan(Map<String, Component> components, long reclaimableBytes) {
		List<HealthAction> actions = new ArrayList<>();
		actions.add(memory(components.get("memory")));
		actions.add(disk(components.get("diskSpace"), reclaimableBytes));
		actions.add(pool(components.get("hikariPool")));
		actions.add(workers(components.get("backgroundWorkers")));
		return actions;
	}

	static HealthAction memory(Component component) {
		double ratio = ratio(num(component, "used"), num(component, "max"));
		String severity = severity(ratio, isDown(component));
		int pct = (int) Math.round(ratio * 100);
		boolean optimize = !"OK".equals(severity);
		return new HealthAction(HealthOptimizeTarget.MEMORY.name(), severity, memoryHeadline(severity, pct),
				"Heap is using " + pct
						+ "% of the JVM max. Optimize clears Spring caches and cancelled jobs, then asks the JVM to reclaim unused heap. GC will not drop heap if the app still holds objects. It will not kill Postgres or HTTP request threads.",
				optimize,
				optimize ? "Clears caches and cancelled jobs, then requests GC. Retained allocations stay until released."
						: "Heap is within a healthy range");
	}

	static HealthAction disk(Component component, long reclaimableBytes) {
		long total = num(component, "total");
		long free = num(component, "free");
		long used = Math.max(0, total - free);
		double ratio = ratio(used, total);
		String severity = severity(ratio, isDown(component));
		int pct = (int) Math.round(ratio * 100);
		boolean optimize = !"OK".equals(severity) && reclaimableBytes > 0;
		String hint;
		if (reclaimableBytes > 0) {
			hint = "About " + formatBytes(reclaimableBytes) + " of KittyP temp/log files can be deleted";
		} else if (!"OK".equals(severity)) {
			hint = "Almost no KittyP temp files to delete — this is the host volume. Free space in Finder or move the database.";
		} else {
			hint = "Disk has spare capacity";
		}
		return new HealthAction(HealthOptimizeTarget.DISK.name(), severity,
				diskHeadline(severity, pct, reclaimableBytes),
				"This volume is " + pct
						+ "% full. Optimize deletes only KittyP temp and log files older than 10 minutes. It cannot wipe the disk, Postgres data, or other apps.",
				optimize, hint);
	}

	static HealthAction pool(Component component) {
		long active = num(component, "active");
		long max = num(component, "max");
		long idle = num(component, "idle");
		long waiting = num(component, "waiting");
		double ratio = ratio(active, max);
		boolean saturated = waiting > 0 && active >= max;
		boolean down = isDown(component) || saturated;
		String severity = waiting > 0 ? "CRITICAL" : severity(ratio, down);
		boolean optimize = !"OK".equals(severity) && idle > 0;
		String hint;
		if (waiting > 0 && idle == 0) {
			hint = "Active queries must finish. Optimize cannot abort them.";
		} else if (optimize) {
			hint = "Evict idle connections only";
		} else {
			hint = "Pool has spare capacity";
		}
		return new HealthAction(HealthOptimizeTarget.POOL.name(), severity,
				poolHeadline(severity, waiting, (int) Math.round(ratio * 100)),
				"Database connections in use: " + active + " of " + max
						+ ". Optimize only drops idle connections. Active queries keep running.",
				optimize, hint);
	}

	static HealthAction workers(Component component) {
		long queue = num(component, "queueSize");
		long cap = num(component, "queueCapacity");
		long cancelled = num(component, "cancelled");
		double ratio = ratio(queue, cap);
		String severity = severity(ratio, isDown(component));
		boolean optimize = !"OK".equals(severity) && cancelled > 0;
		String hint;
		if (!"OK".equals(severity) && cancelled == 0) {
			hint = "Running jobs must finish. Stop load or wait. Optimize cannot kill live work.";
		} else if (optimize) {
			hint = "Purge cancelled jobs only";
		} else {
			hint = "Worker queue is healthy";
		}
		return new HealthAction(HealthOptimizeTarget.WORKERS.name(), severity,
				workersHeadline(severity, (int) Math.round(ratio * 100)),
				"Background queue is " + queue + " of " + cap
						+ ". Optimize removes cancelled jobs only. Running HTTP requests and live async work are left alone.",
				optimize, hint);
	}

	static String severity(double ratio, boolean down) {
		if (down || ratio >= CRITICAL_RATIO) {
			return "CRITICAL";
		}
		if (ratio >= WATCH_RATIO) {
			return "WATCH";
		}
		return "OK";
	}

	static double ratio(long used, long max) {
		if (max <= 0) {
			return 0;
		}
		return (double) used / max;
	}

	static long num(Component component, String key) {
		if (component == null || component.details() == null) {
			return 0;
		}
		Object value = component.details().get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		return 0;
	}

	static boolean isDown(Component component) {
		return component != null && "DOWN".equals(component.status());
	}

	private static String memoryHeadline(String severity, int pct) {
		if ("CRITICAL".equals(severity)) {
			return "Heap is " + pct + "% of JVM max";
		}
		if ("WATCH".equals(severity)) {
			return "Heap usage is elevated (" + pct + "%)";
		}
		return "Memory is healthy (" + pct + "%)";
	}

	private static String diskHeadline(String severity, int pct, long reclaimableBytes) {
		if ("CRITICAL".equals(severity)) {
			return reclaimableBytes > 0 ? "Disk is " + pct + "% full — KittyP can free some temp files"
					: "Disk is " + pct + "% full — host volume, not KittyP heap";
		}
		if ("WATCH".equals(severity)) {
			return "Disk usage is high (" + pct + "%)";
		}
		return "Disk has free space (" + pct + "% used)";
	}

	private static String poolHeadline(String severity, long waiting, int pct) {
		if (waiting > 0) {
			return "Database pool is saturated (" + waiting + " waiting)";
		}
		if ("CRITICAL".equals(severity)) {
			return "Database pool is " + pct + "% busy";
		}
		if ("WATCH".equals(severity)) {
			return "Database pool usage is elevated (" + pct + "%)";
		}
		return "Database pool is healthy";
	}

	private static String workersHeadline(String severity, int pct) {
		if ("CRITICAL".equals(severity)) {
			return "Background queue is " + pct + "% full";
		}
		if ("WATCH".equals(severity)) {
			return "Background queue is filling (" + pct + "%)";
		}
		return "Background workers are healthy";
	}

	public static String formatBytes(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}
		double kb = bytes / 1024.0;
		if (kb < 1024) {
			return (kb >= 10 ? Math.round(kb) + " KB" : String.format("%.1f KB", kb));
		}
		double mb = kb / 1024.0;
		if (mb < 1024) {
			return (mb >= 10 ? Math.round(mb) + " MB" : String.format("%.1f MB", mb));
		}
		return String.format("%.1f GB", mb / 1024.0);
	}
}
