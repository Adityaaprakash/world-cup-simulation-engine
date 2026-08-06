package com.aditya.worldcup.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentStatisticsResponse {
    private Long tournamentId;
    private String tournamentName;
    private int year;
    private String status;
    private int completedMatches;
    
    private int totalGoals;
    private double averageGoals;
    private String biggestWin;
    private int cleanSheets;
    private int penalties;
    private int yellowCards;
    private int redCards;
    private Long attendance;
    private String highestScoringMatch;
    
    private String topScorer;
    private String bestGoalkeeper;
    private String champion;
}
