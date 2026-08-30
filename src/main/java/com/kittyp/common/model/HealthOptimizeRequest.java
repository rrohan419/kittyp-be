package com.kittyp.common.model;

import jakarta.validation.constraints.NotNull;

public record HealthOptimizeRequest(@NotNull HealthOptimizeTarget target) {
}
