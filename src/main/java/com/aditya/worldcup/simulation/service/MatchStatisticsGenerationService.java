package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.simulation.dto.MatchStatisticsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Service
public class MatchStatisticsGenerationService {

    private final Random random = new Random();

    public MatchStatisticsResponse generate(
            int homeGoals,
            int awayGoals,
            int homeStrength,
            int awayStrength
    ) {

        int resultInfluence = Integer.compare(homeGoals, awayGoals) * 4;
        int strengthInfluence = clamp(
                (homeStrength - awayStrength) / 3,
                -8,
                8
        );

        int homePossession = clamp(
                50 + resultInfluence + strengthInfluence + randomRange(-6, 6),
                35,
                65
        );
        int awayPossession = 100 - homePossession;

        int homeShots = generateShots(
                homeGoals,
                homeStrength,
                awayStrength,
                homeGoals > awayGoals
        );
        int awayShots = generateShots(
                awayGoals,
                awayStrength,
                homeStrength,
                awayGoals > homeGoals
        );

        int homeShotsOnTarget = generateShotsOnTarget(
                homeShots,
                homeGoals
        );
        int awayShotsOnTarget = generateShotsOnTarget(
                awayShots,
                awayGoals
        );

        int homePassAccuracy = generatePassAccuracy(
                homePossession,
                homeGoals > awayGoals,
                homeStrength
        );
        int awayPassAccuracy = generatePassAccuracy(
                awayPossession,
                awayGoals > homeGoals,
                awayStrength
        );

        int homePasses = generatePasses(
                homePossession,
                homePassAccuracy,
                homeStrength
        );
        int awayPasses = generatePasses(
                awayPossession,
                awayPassAccuracy,
                awayStrength
        );

        return new MatchStatisticsResponse(
                new MatchStatisticsResponse.TeamStatisticsResponse(
                        homePossession,
                        homeShots,
                        homeShotsOnTarget,
                        homePasses,
                        homePassAccuracy,
                        generateCorners(homeShots),
                        generateFouls(homeGoals < awayGoals),
                        randomRange(0, 4),
                        generateYellowCards(),
                        generateRedCards(),
                        generateSaves(awayShotsOnTarget, awayGoals),
                        generateExpectedGoals(homeGoals, homeShotsOnTarget)
                ),
                new MatchStatisticsResponse.TeamStatisticsResponse(
                        awayPossession,
                        awayShots,
                        awayShotsOnTarget,
                        awayPasses,
                        awayPassAccuracy,
                        generateCorners(awayShots),
                        generateFouls(awayGoals < homeGoals),
                        randomRange(0, 4),
                        generateYellowCards(),
                        generateRedCards(),
                        generateSaves(homeShotsOnTarget, homeGoals),
                        generateExpectedGoals(awayGoals, awayShotsOnTarget)
                )
        );
    }

    private int generateShots(
            int goals,
            int attackingStrength,
            int defendingStrength,
            boolean winner
    ) {

        int strengthEdge = clamp(
                (attackingStrength - defendingStrength) / 4,
                -3,
                3
        );
        int winnerBoost = winner ? randomRange(1, 3) : 0;

        return clamp(
                7 + goals * 2 + strengthEdge + winnerBoost
                        + randomRange(-3, 5),
                Math.max(5, goals + 4),
                20
        );
    }

    private int generateShotsOnTarget(int shots, int goals) {

        int minimum = Math.max(goals, 2);
        int target = goals + randomRange(2, 6);

        return clamp(
                target,
                minimum,
                Math.min(shots, 10)
        );
    }

    private int generatePassAccuracy(
            int possession,
            boolean winner,
            int strength
    ) {

        int winnerBoost = winner ? randomRange(1, 3) : 0;

        return clamp(
                74 + (possession - 50) / 3 + (strength - 75) / 5
                        + winnerBoost + randomRange(-4, 5),
                70,
                95
        );
    }

    private int generatePasses(
            int possession,
            int passAccuracy,
            int strength
    ) {

        return clamp(
                320 + (possession - 45) * 8 + (passAccuracy - 75) * 4
                        + (strength - 75) * 3 + randomRange(-45, 65),
                250,
                700
        );
    }

    private int generateCorners(int shots) {

        return clamp(
                shots / 3 + randomRange(-2, 3),
                0,
                10
        );
    }

    private int generateFouls(boolean losing) {

        int losingBoost = losing ? randomRange(1, 3) : 0;

        return clamp(
                randomRange(7, 15) + losingBoost,
                5,
                18
        );
    }

    private int generateYellowCards() {

        int roll = random.nextInt(100);

        if (roll < 12) {
            return 0;
        }

        if (roll < 72) {
            return randomRange(1, 2);
        }

        return randomRange(3, 5);
    }

    private int generateRedCards() {

        return random.nextInt(100) < 8 ? 1 : 0;
    }

    private int generateSaves(int opponentShotsOnTarget, int opponentGoals) {

        return clamp(
                opponentShotsOnTarget - opponentGoals + randomRange(-1, 1),
                0,
                8
        );
    }

    private double generateExpectedGoals(int goals, int shotsOnTarget) {

        double value = 0.45 + goals * 0.65 + shotsOnTarget * 0.18
                + randomRange(-2, 4) * 0.1;

        return BigDecimal.valueOf(clamp(value, 0.2, 4.5))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private int randomRange(int minimum, int maximum) {

        return random.nextInt(maximum - minimum + 1) + minimum;
    }

    private int clamp(int value, int minimum, int maximum) {

        return Math.max(minimum, Math.min(maximum, value));
    }

    private double clamp(double value, double minimum, double maximum) {

        return Math.max(minimum, Math.min(maximum, value));
    }
}
