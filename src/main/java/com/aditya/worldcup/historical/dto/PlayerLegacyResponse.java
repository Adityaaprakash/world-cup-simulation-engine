package com.aditya.worldcup.historical.dto;

public record PlayerLegacyResponse(Long playerId, String playerName, String nation,
        long legacyScore, int trophiesWon, int goldenBoots, int goldenBalls,
        int goldenGloves, int motmAwards, int worldCupAppearances,
        int finalsPlayed, int finalsWon, int captainAppearances,
        int tournamentRecordsHeld) {
}
