package com.kittyp.common.model;

public record HealthOptimizeResponse(String target, boolean applied, String summary, String remainingHint,
		SystemHealthResponse before, SystemHealthResponse after) {
}
