package com.aditya.worldcup.simulation.dto;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;

import java.util.List;

public record MatchSimulationResponse(
        String homeTeam,
        String awayTeam,
        Integer homeGoals,
        Integer awayGoals,
        String winner,
        Integer homeStrength,
        Integer awayStrength,
        List<MatchEventResponse> events,
        MatchStatisticsResponse statistics,
        List<PlayerMatchRatingResponse> playerRatings
) {
}
