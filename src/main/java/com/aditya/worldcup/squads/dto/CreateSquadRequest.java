package com.aditya.worldcup.squads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateSquadRequest(
        @NotNull(message = "Team id is required")
        @Positive(message = "Team id must be positive")
        Long teamId,

        @NotNull(message = "Formation id is required")
        @Positive(message = "Formation id must be positive")
        Long formationId,

        @NotBlank(message = "Squad name is required")
        @Size(max = 100, message = "Squad name must be at most 100 characters")
        String name
) {
}
