package com.aditya.worldcup.squadplayers.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StartingXiRequest(

        @NotEmpty(message = "Starting XI player ids are required")
        @Size(min = 11, max = 11, message = "Starting XI must contain exactly 11 players")
        List<@Positive(message = "Player ids must be positive") Long> playerIds

) {
}
