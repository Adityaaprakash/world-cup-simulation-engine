package com.aditya.worldcup.simulation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatchSimulationRequest(
        @NotNull(message = "Home squad id is required")
        @Positive(message = "Home squad id must be positive")
        Long homeSquadId,

        @NotNull(message = "Away squad id is required")
        @Positive(message = "Away squad id must be positive")
        Long awaySquadId
) {
}
