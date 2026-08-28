package com.kittyp.common.model;

import java.util.Map;

public record SystemHealthResponse(String status, Map<String, Component> components) {

	public record Component(String status, Map<String, Object> details) {
	}
}
