package com.aditya.worldcup.tournaments.dto;

import java.util.List;

public record TournamentSummaryResponse(
        Long tournamentId,
        String currentStage,
        Long remainingFixtures,
        String biggestUpset,
        String mostEntertainingMatch,
        String highestScoringMatch,
        String topScorer,
        String bestGoalkeeper,
        String championPath,
        String longestStreak,
        Integer totalGoals,
        Integer completedMatches,
        List<MatchNarrativeResponse> narratives,
        TournamentTeamAwardsResponse teamAwards
) {
}
