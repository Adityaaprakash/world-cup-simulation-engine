package com.aditya.worldcup.simulation.dto;

public record PlayerMatchRatingResponse(
        Long playerId,
        String playerName,
        String position,
        String team,
        Double rating
) {
}
