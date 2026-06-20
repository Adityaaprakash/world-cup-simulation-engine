package com.aditya.worldcup.simulation.dto;

public record MatchSimulationResponse(
        String homeTeam,
        String awayTeam,
        Integer homeGoals,
        Integer awayGoals,
        String winner,
        Integer homeStrength,
        Integer awayStrength
) {
}