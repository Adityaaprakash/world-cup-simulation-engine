package com.aditya.worldcup.matches.dto;

import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;

public record MatchResponse(
        Long id,
        String group,
        MatchRound round,
        String homeTeam,
        String awayTeam,
        MatchStatus status
) {
}
