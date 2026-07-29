package com.aditya.worldcup.saves.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateSaveSlotRequest(
        @Size(max = 100, message = "slotName must be at most 100 characters")
        String slotName,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @Positive(message = "currentTournamentId must be positive")
        Long currentTournamentId,

        @PositiveOrZero(message = "currentSeason must be zero or positive")
        Integer currentSeason,

        @Size(max = 80, message = "currentStage must be at most 80 characters")
        String currentStage,

        @PositiveOrZero(message = "totalPlayTime must be zero or positive")
        Long totalPlayTime
) {
}
