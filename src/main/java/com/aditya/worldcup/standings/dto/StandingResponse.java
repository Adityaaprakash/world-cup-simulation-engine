package com.aditya.worldcup.standings.dto;

public record StandingResponse(
        String team,
        Integer points,
        Integer wins,
        Integer draws,
        Integer losses,
        Integer goalsFor,
        Integer goalsAgainst,
        Integer goalDifference
) {
}
