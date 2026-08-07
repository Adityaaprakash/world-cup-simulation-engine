package com.aditya.worldcup.historical.dto;

public record HistoricalSummaryResponse(long playersProfiled, long teamsProfiled,
        long managersProfiled, long completedTournaments, long recordedMatches,
        String greatestPlayer, String greatestTeam, String greatestManager) {
}
