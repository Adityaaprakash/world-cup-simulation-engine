package com.aditya.worldcup.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticsResponse {
    private Long id;
    private String name;
    private String country;
    private String position;
    private int overallRating;

    // Career
    private int goals;
    private int assists;
    private int cleanSheets;
    private int motmAwards;
    private double averageRating;
    private int minutesPlayed;

    // Discipline
    private int yellowCards;
    private int redCards;

    // Attack
    private int shots;
    private int shotsOnTarget;
    private int penaltiesScored;
    private int penaltiesMissed;
    private int ownGoals;

    // Passing
    private double passAccuracy;
    private int keyPasses;

    // Defending
    private int tackles;
    private int interceptions;
    private int blocks;

    // Goalkeeping
    private int saves;
    private double savePercentage;
}
