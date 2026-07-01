package com.aditya.worldcup.simulation.dto;

public record ManOfTheMatchResponse(
        Long playerId,
        String playerName,
        String team,
        String position,
        Double rating
) {
}
