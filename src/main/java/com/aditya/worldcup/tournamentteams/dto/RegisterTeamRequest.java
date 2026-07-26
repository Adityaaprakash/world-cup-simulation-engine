package com.aditya.worldcup.tournamentteams.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterTeamRequest(

        @NotNull(message = "Team id is required")
        @Positive(message = "Team id must be positive")
        Long teamId

) {
}
