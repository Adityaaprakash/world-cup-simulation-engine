package com.aditya.worldcup.ml.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PredictionResponse(
        double homeWinProbability,
        double drawProbability,
        double awayWinProbability,
        double expectedHomeGoals,
        double expectedAwayGoals
) {
}
