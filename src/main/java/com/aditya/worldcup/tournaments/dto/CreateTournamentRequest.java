package com.aditya.worldcup.tournaments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CreateTournamentRequest(

        @NotBlank(message = "Tournament name is required")
        @Size(max = 120, message = "Tournament name must be at most 120 characters")
        String name,

        @NotNull(message = "Tournament year is required")
        @Min(value = 1930, message = "Tournament year must be 1930 or later")
        @Max(value = 2100, message = "Tournament year must be 2100 or earlier")
        Integer year,

        @NotBlank(message = "Host country is required")
        @Size(max = 80, message = "Host country must be at most 80 characters")
        String hostCountry

) {
}
