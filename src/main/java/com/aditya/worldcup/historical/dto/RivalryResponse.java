package com.aditya.worldcup.historical.dto;

public record RivalryResponse(String type, Long firstId, String firstName,
        Long secondId, String secondName, int meetings, int firstWins,
        int draws, int secondWins, int firstGoals, int secondGoals,
        int biggestVictory, int finalsPlayed) {
}
