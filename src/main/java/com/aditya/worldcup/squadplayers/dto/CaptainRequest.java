package com.aditya.worldcup.squadplayers.dto;

import jakarta.validation.constraints.NotNull;

public record CaptainRequest(

        @NotNull
        Long playerId

) {
}