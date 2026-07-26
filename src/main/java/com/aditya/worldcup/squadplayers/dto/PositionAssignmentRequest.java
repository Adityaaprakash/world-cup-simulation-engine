package com.aditya.worldcup.squadplayers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PositionAssignmentRequest(

        @NotNull(message = "Player id is required")
        @Positive(message = "Player id must be positive")
        Long playerId,

        @NotBlank(message = "Position slot is required")
        @Pattern(
                regexp = "GK|LB|CB|RB|LWB|RWB|CDM|CM|CAM|LM|RM|LW|RW|ST|CF",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Invalid position slot"
        )
        String positionSlot

) {
}
