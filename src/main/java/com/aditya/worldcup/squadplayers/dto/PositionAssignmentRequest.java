package com.aditya.worldcup.squadplayers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PositionAssignmentRequest(

        @NotNull
        Long playerId,

        @NotBlank
        String positionSlot

) {
}