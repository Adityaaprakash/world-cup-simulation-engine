package com.aditya.worldcup.search.dto;

import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import java.util.List;

public record TournamentSearchRequest(
        String name, TournamentStatus status, MatchRound stage, Integer year,
        String champion, Boolean archived, Integer page, Integer size,
        List<SearchSort> sort
) {
}
