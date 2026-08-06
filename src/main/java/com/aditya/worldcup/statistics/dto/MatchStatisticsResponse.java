package com.aditya.worldcup.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchStatisticsResponse {
    private Long matchId;
    private Long tournamentId;
    private String tournamentName;
    private String round;
    private LocalDateTime matchDate;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private String status;
    
    private double homePossession;
    private double awayPossession;
    private int homeShots;
    private int awayShots;
    private int homeShotsOnTarget;
    private int awayShotsOnTarget;
    private int homePasses;
    private int awayPasses;
    private double homePassAccuracy;
    private double awayPassAccuracy;
    private int homeCorners;
    private int awayCorners;
    private int homeFouls;
    private int awayFouls;
    private int homeYellowCards;
    private int awayYellowCards;
    private int homeRedCards;
    private int awayRedCards;
    private int homeSaves;
    private int awaySaves;
    private double homeExpectedGoals;
    private double awayExpectedGoals;
}
