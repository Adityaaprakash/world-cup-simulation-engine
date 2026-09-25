package com.aditya.worldcup.tactics.dto;

import com.aditya.worldcup.simulation.dto.TeamStrengthResponse;
import lombok.Builder;
import java.util.List;

@Builder
public record MatchPreparationResponse(
        Long matchId,
        Long opponentSquadId,
        String opponentName,
        String opponentFormation,
        TeamStrengthResponse opponentStrength,
        List<String> fitnessWarnings,
        MatchPlanDto currentPlan
) {}
