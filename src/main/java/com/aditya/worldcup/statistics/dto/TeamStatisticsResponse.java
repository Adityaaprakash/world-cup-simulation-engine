package com.aditya.worldcup.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatisticsResponse {
    private Long teamId;
    private String teamName;
    
    private int matchesPlayed;
    private int wins;
    private int draws;
    private int losses;
    private double winPercentage;
    
    private int goalsScored;
    private int goalsConceded;
    private int cleanSheets;
    
    private double averagePossession;
    private double averageExpectedGoals;
    private double passAccuracy;
    private double averagePlayerRating;
    
    private Map<String, Long> formationUsage;
}
