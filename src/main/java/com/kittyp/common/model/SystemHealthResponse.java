package com.kittyp.common.model;

import java.util.List;
import java.util.Map;

public record SystemHealthResponse(String status, Map<String, Component> components, List<HealthAction> actions,
		boolean loadTestEnabled, List<String> loadTestActive) {

	public SystemHealthResponse {
		actions = actions == null ? List.of() : List.copyOf(actions);
		loadTestActive = loadTestActive == null ? List.of() : List.copyOf(loadTestActive);
	}

	public SystemHealthResponse(String status, Map<String, Component> components) {
		this(status, components, List.of(), false, List.of());
	}

	public SystemHealthResponse(String status, Map<String, Component> components, List<HealthAction> actions) {
		this(status, components, actions, false, List.of());
	}

	public record Component(String status, Map<String, Object> details) {
	}

	public record HealthAction(String target, String severity, String headline, String detail, boolean optimizeEnabled,
			String optimizeHint) {
	}
}
