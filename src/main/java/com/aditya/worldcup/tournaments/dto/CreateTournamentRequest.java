package com.aditya.worldcup.tournaments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTournamentRequest(

        @NotBlank
        String name,

        @NotNull
        Integer year,

        @NotBlank
        String hostCountry

) {
}