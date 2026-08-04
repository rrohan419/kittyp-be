package com.kittyp.user.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PetWeightLogRequest(
        @NotNull @Positive Double weight,
        LocalDateTime recordedAt,
        String note) {
}
