package com.aditya.worldcup.historical.dto;

public record HeadToHeadResponse(String type, Long firstId, String firstName,
        Long secondId, String secondName, int matches, int firstWins,
        int draws, int secondWins, int firstGoals, int secondGoals,
        int firstTrophiesWonAgainst, int secondTrophiesWonAgainst) {
}
