package com.aditya.worldcup.ml.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PredictionRequest(
        double homeAttackStrength,
        double awayAttackStrength,
        double homeMidfieldStrength,
        double awayMidfieldStrength,
        double homeDefenseStrength,
        double awayDefenseStrength,
        double homeGoalkeeperStrength,
        double awayGoalkeeperStrength,
        double homeFifaRank,
        double awayFifaRank,
        double fifaRankDifference,
        int neutralGround,
        int year,
        int month,
        double homeLast5Wins,
        double awayLast5Wins,
        double homeLast5Goals,
        double awayLast5Goals,
        double homeLast5GoalDifference,
        double awayLast5GoalDifference,
        double homeLast5GoalsConceded,
        double awayLast5GoalsConceded,
        int homeTeamEncoded,
        int awayTeamEncoded,
        int tournamentEncoded,
        int tournamentTypeEncoded,
        int venueEncoded
) {
}
