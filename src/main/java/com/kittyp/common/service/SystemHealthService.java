package com.kittyp.common.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.stereotype.Service;

import com.kittyp.common.model.SystemHealthResponse;
import com.kittyp.common.model.SystemHealthResponse.Component;

@Service
public class SystemHealthService {

	private final Map<String, HealthIndicator> indicators;
	private final ObjectProvider<DataSourceHealthIndicator> dbIndicator;

	public SystemHealthService(Map<String, HealthIndicator> indicators,
			ObjectProvider<DataSourceHealthIndicator> dbIndicator) {
		this.indicators = indicators;
		this.dbIndicator = dbIndicator;
	}

	public SystemHealthResponse snapshot() {
		Map<String, Health> healths = new LinkedHashMap<>();
		indicators.forEach((beanName, indicator) -> healths.put(contributorName(beanName), indicator.health()));
		DataSourceHealthIndicator db = dbIndicator.getIfAvailable();
		if (db != null) {
			healths.putIfAbsent("db", db.health());
		}
		return assemble(healths);
	}

	static SystemHealthResponse assemble(Map<String, Health> healths) {
		Set<Status> statuses = new LinkedHashSet<>();
		Map<String, Component> components = new LinkedHashMap<>();
		healths.forEach((name, health) -> {
			statuses.add(health.getStatus());
			components.put(name, new Component(health.getStatus().getCode(), jsonSafeMap(health.getDetails())));
		});
		String overall = statuses.isEmpty() ? Status.UNKNOWN.getCode()
				: StatusAggregator.getDefault().getAggregateStatus(statuses).getCode();
		return new SystemHealthResponse(overall, components);
	}

	static String contributorName(String beanName) {
		String name = beanName;
		if (name.endsWith("HealthIndicator")) {
			name = name.substring(0, name.length() - "HealthIndicator".length());
		} else if (name.endsWith("HealthContributor")) {
			name = name.substring(0, name.length() - "HealthContributor".length());
		}
		return name;
	}

	static Map<String, Object> jsonSafeMap(Map<String, Object> details) {
		Map<String, Object> copy = new LinkedHashMap<>();
		if (details == null) {
			return copy;
		}
		details.forEach((key, value) -> copy.put(key, jsonSafe(value)));
		return copy;
	}

	private static Object jsonSafe(Object value) {
		if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
			return value;
		}
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> nested = new LinkedHashMap<>();
			map.forEach((nestedKey, nestedValue) -> nested.put(String.valueOf(nestedKey), jsonSafe(nestedValue)));
			return nested;
		}
		if (value instanceof Iterable<?> iterable) {
			List<Object> list = new ArrayList<>();
			iterable.forEach(item -> list.add(jsonSafe(item)));
			return list;
		}
		return String.valueOf(value);
	}
}
