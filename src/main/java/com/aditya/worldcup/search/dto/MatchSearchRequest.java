package com.aditya.worldcup.search.dto;

import com.aditya.worldcup.matches.entity.MatchRound;
import java.time.LocalDateTime;
import java.util.List;

public record MatchSearchRequest(
        String homeTeam, String awayTeam, String tournament, MatchRound stage,
        Integer homeScore, Integer awayScore, String player,
        LocalDateTime fromDate, LocalDateTime toDate,
        Integer page, Integer size, List<SearchSort> sort
) {
}
