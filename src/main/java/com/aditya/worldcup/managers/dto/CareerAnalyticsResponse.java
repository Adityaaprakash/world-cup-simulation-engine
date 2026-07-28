package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.CoachingStyle;

public record CareerAnalyticsResponse(
        Long managerId,
        Double winPercentage,
        Double averageGoalsScored,
        Double averageGoalsConceded,
        Double averagePossession,
        String favoriteFormation,
        String favoriteTactics,
        CoachingStyle tacticalProfile,
        String mostUsedLineup,
        String mostSelectedCaptain,
        String mostTrustedPlayers,
        Integer longestUnbeatenStreak
) {
}
