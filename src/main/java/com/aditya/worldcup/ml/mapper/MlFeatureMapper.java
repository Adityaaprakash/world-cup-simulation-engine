package com.aditya.worldcup.ml.mapper;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.ml.dto.PredictionRequest;
import com.aditya.worldcup.simulation.dto.TeamStrengthResponse;
import com.aditya.worldcup.squads.entity.Squad;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class MlFeatureMapper {

    public PredictionRequest map(
            Squad homeSquad,
            Squad awaySquad,
            TeamStrengthResponse homeStrength,
            TeamStrengthResponse awayStrength,
            Match match
    ) {
        YearMonth currentMonth = YearMonth.now();
        double homeRank = fifaRank(homeSquad);
        double awayRank = fifaRank(awaySquad);
        int tournamentCode = match != null && match.getTournament() != null
                ? encodedId(match.getTournament().getId())
                : 0;

        return new PredictionRequest(
                chemistryAdjusted(homeStrength.attack(), homeStrength.chemistry()) * formationAttackMultiplier(homeSquad),
                chemistryAdjusted(awayStrength.attack(), awayStrength.chemistry()) * formationAttackMultiplier(awaySquad),
                chemistryAdjusted(homeStrength.midfield(), homeStrength.chemistry()) * formationMidfieldMultiplier(homeSquad),
                chemistryAdjusted(awayStrength.midfield(), awayStrength.chemistry()) * formationMidfieldMultiplier(awaySquad),
                chemistryAdjusted(homeStrength.defense(), homeStrength.chemistry()),
                chemistryAdjusted(awayStrength.defense(), awayStrength.chemistry()),
                chemistryAdjusted(homeStrength.goalkeeper(), homeStrength.chemistry()),
                chemistryAdjusted(awayStrength.goalkeeper(), awayStrength.chemistry()),
                homeRank, awayRank, awayRank - homeRank, 0,
                currentMonth.getYear(), currentMonth.getMonthValue(),
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                encodedId(homeSquad.getTeam().getId()),
                encodedId(awaySquad.getTeam().getId()),
                tournamentCode, tournamentCode, 0
        );
    }

    private double chemistryAdjusted(Integer rating, Integer chemistry) {
        return rating * (chemistry / 100.0);
    }

    private double formationAttackMultiplier(Squad squad) {
        return 1.0 + (squad.getFormation().getAttackers() - 3) * 0.03;
    }

    private double formationMidfieldMultiplier(Squad squad) {
        return 1.0 + (squad.getFormation().getMidfielders() - 3) * 0.03;
    }

    private double fifaRank(Squad squad) {
        return squad.getTeam().getCountry().getFifaRanking();
    }

    private int encodedId(Long id) {
        return Math.toIntExact(id);
    }
}
