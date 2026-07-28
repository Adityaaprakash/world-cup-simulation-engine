package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.ManagerReputation;

import java.util.List;

public record ManagerLeaderboardsResponse(
        List<ManagerLeaderboardEntry> highestWinRate,
        List<ManagerLeaderboardEntry> mostTrophies,
        List<ManagerLeaderboardEntry> mostMatches,
        List<ManagerLeaderboardEntry> longestUnbeatenStreak,
        List<ManagerLeaderboardEntry> highestReputation
) {

    public record ManagerLeaderboardEntry(
            Long managerId,
            String displayName,
            ManagerReputation reputation,
            Integer level,
            Double winPercentage,
            Integer trophiesWon,
            Integer matchesManaged,
            Integer longestUnbeatenStreak
    ) {
    }
}
