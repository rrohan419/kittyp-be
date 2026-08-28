package com.kittyp.user.dto;

import java.time.LocalDateTime;

import com.kittyp.user.enums.PetReminderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PetReminderDtos {

    private PetReminderDtos() {
    }

    public record PetReminderRequest(
            @NotBlank String petUuid,
            @NotNull PetReminderType type,
            @NotNull LocalDateTime dueAt,
            String note,
            Boolean pushEnabled,
            Boolean whatsappEnabled) {
    }

    public record PetReminderUpdateRequest(
            PetReminderType type,
            LocalDateTime dueAt,
            String note,
            Boolean pushEnabled,
            Boolean whatsappEnabled,
            Boolean isActive) {
    }

    public record PetReminderModel(
            String uuid,
            String petUuid,
            String petName,
            PetReminderType type,
            LocalDateTime dueAt,
            String note,
            Boolean pushEnabled,
            Boolean whatsappEnabled,
            LocalDateTime sentAt,
            Boolean isActive) {
    }
}
