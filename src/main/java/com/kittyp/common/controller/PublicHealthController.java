package com.kittyp.common.controller;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicHealthController {

	private final List<HealthIndicator> indicators;
	private final StatusAggregator statusAggregator = StatusAggregator.getDefault();

	public PublicHealthController(List<HealthIndicator> indicators) {
		this.indicators = indicators;
	}

	@GetMapping({ "/health", "/actuator/health" })
	public ResponseEntity<Map<String, String>> health() {
		Status status = aggregate();
		HttpStatus http = Status.UP.equals(status) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
		return ResponseEntity.status(http).body(Map.of("status", status.getCode()));
	}

	private Status aggregate() {
		Set<Status> statuses = new LinkedHashSet<>();
		for (HealthIndicator indicator : indicators) {
			statuses.add(indicator.health().getStatus());
		}
		if (statuses.isEmpty()) {
			return Status.UP;
		}
		return statusAggregator.getAggregateStatus(statuses);
	}
}
