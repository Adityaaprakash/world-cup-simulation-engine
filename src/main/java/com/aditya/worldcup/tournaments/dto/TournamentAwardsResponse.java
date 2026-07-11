package com.aditya.worldcup.tournaments.dto;

import java.util.List;

public record TournamentAwardsResponse(
        GoldenBootAward goldenBoot,
        RatingAward goldenBall,
        RatingAward goldenGlove,
        RatingAward bestYoungPlayer,
        List<TeamOfTournamentPlayer> teamOfTheTournament
) {

    public record GoldenBootAward(
            Long playerId,
            String player,
            String team,
            Integer goals
    ) {
    }

    public record RatingAward(
            Long playerId,
            String player,
            String team,
            String position,
            Double averageRating
    ) {
    }

    public record TeamOfTournamentPlayer(
            Long playerId,
            String player,
            String team,
            String position,
            String slot,
            Double averageRating
    ) {
    }
}
