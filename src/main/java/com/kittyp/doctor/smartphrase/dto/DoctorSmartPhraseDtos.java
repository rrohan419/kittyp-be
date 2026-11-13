package com.kittyp.doctor.smartphrase.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DoctorSmartPhraseDtos {

    private DoctorSmartPhraseDtos() {
    }

    public record SmartPhraseRequest(
            @NotBlank @Size(max = 4000) String phraseText) {
    }

    public record SmartPhraseModel(
            String uuid,
            String phraseText,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
