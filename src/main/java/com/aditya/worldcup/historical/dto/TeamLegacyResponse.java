package com.aditya.worldcup.historical.dto;

public record TeamLegacyResponse(Long teamId, String teamName, long legacyScore,
        int worldCupTitles, int finals, int semiFinals, int totalWins,
        double winPercentage, int historicalGoals, int cleanSheets,
        int biggestVictory, int longestUnbeatenRun, int longestWinningStreak) {
}
