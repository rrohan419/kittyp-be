package com.kittyp.nutrition.dto;

import java.time.LocalDateTime;

import com.kittyp.nutrition.enums.FeedingStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PetFeedingLogRequest(
        Long dailyPlanId,
        @NotNull FeedingStatus status,
        @PositiveOrZero Double quantity,
        String notes,
        LocalDateTime loggedAt) {
}
