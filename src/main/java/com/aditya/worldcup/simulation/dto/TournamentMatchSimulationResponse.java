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
        Boolean wentToExtraTime,
        Boolean wentToPenalties,
        Integer homePenaltiesScore,
        Integer awayPenaltiesScore,
        String status,
        List<MatchEventResponse> events,
        MatchStatisticsResponse statistics,
        List<PlayerMatchRatingResponse> playerRatings,
        ManOfTheMatchResponse manOfTheMatch,
        List<CommentaryResponse> commentary
) {
}
