package com.aditya.worldcup.squadplayers.dto;

public record SquadAnalysisResponse(
        int totalPlayers,
        long goalkeeperCount,
        long defenderCount,
        long midfielderCount,
        long attackerCount,
        long unavailableCount,
        String recommendation,
        boolean validSize
) {}
