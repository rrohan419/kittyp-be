package com.kittyp.user.models;

import java.time.LocalDateTime;

public record PetWeightLogModel(
        Long id,
        Double weight,
        LocalDateTime recordedAt,
        String note) {
}
