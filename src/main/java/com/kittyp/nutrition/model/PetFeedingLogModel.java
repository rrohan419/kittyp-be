package com.kittyp.nutrition.model;

import java.time.LocalDateTime;

import com.kittyp.nutrition.enums.FeedingStatus;

public record PetFeedingLogModel(
        Long id,
        Long dailyPlanId,
        FeedingStatus status,
        Double quantity,
        String notes,
        LocalDateTime loggedAt) {
}
