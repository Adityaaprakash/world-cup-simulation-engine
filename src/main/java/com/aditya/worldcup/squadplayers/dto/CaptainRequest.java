package com.aditya.worldcup.squadplayers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CaptainRequest(

        @NotNull(message = "Player id is required")
        @Positive(message = "Player id must be positive")
        Long playerId

) {
}
