package com.aditya.worldcup.matches.dto;

import com.aditya.worldcup.matches.entity.MatchStatus;
import java.time.LocalDateTime;

public record MatchHistoryResponse(
        Long matchId,
        String homeTeam,
        String awayTeam,
        String score,
        String winner,
        MatchStatus status,
        LocalDateTime matchDate
) {
}
