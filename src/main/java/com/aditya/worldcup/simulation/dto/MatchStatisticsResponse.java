package com.aditya.worldcup.simulation.dto;

public record MatchStatisticsResponse(
        TeamStatisticsResponse homeTeam,
        TeamStatisticsResponse awayTeam
) {

    public record TeamStatisticsResponse(
            Integer possession,
            Integer shots,
            Integer shotsOnTarget,
            Integer passes,
            Integer passAccuracy,
            Integer corners,
            Integer fouls,
            Integer offsides,
            Integer yellowCards,
            Integer redCards,
            Integer saves,
            Double expectedGoals
    ) {
    }
}
