package com.aditya.worldcup.simulation.dto;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;

import java.util.List;

public record TournamentMatchSimulationResponse(
        Long matchId,
        String homeTeam,
        String awayTeam,
        Integer homeGoals,
        Integer awayGoals,
        String winner,
        String status,
        List<MatchEventResponse> events,
        MatchStatisticsResponse statistics,
        List<PlayerMatchRatingResponse> playerRatings,
        ManOfTheMatchResponse manOfTheMatch
) {
}
