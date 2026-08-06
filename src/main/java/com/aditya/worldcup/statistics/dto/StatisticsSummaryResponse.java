package com.aditya.worldcup.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsSummaryResponse {
    private long totalTournaments;
    private long totalTeams;
    private long totalPlayers;
    private long totalMatchesSimulated;
    private long totalGoalsScored;
    private double averageGoalsPerMatch;
    private long activeManagers;
}
