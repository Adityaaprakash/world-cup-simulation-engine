package com.aditya.worldcup.matches.dto;

import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.simulation.dto.MatchStatisticsResponse;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.PlayerMatchRatingResponse;
import com.aditya.worldcup.simulation.dto.ManOfTheMatchResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;

import java.util.List;

public record MatchDetailResponse(
        Long matchId,
        String group,
        MatchRound round,
        String homeTeam,
        String awayTeam,
        Integer homeScore,
        Integer awayScore,
        String winner,
        MatchStatus status,
        MatchStatisticsResponse statistics,
        List<MatchEventResponse> events,
        List<PlayerMatchRatingResponse> playerRatings,
        ManOfTheMatchResponse manOfTheMatch,
        List<CommentaryResponse> commentary
) {
}
