package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.teams.entity.Team;

public record TournamentUpset(
        Team winner,
        Team loser,
        MatchRound stage,
        int ratingDifference
) {
}
