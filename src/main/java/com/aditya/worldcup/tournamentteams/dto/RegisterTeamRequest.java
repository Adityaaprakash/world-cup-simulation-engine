package com.aditya.worldcup.tournamentteams.dto;

import jakarta.validation.constraints.NotNull;

public record RegisterTeamRequest(

        @NotNull
        Long teamId

) {
}