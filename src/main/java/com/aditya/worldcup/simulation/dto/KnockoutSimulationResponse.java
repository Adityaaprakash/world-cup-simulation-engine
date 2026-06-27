package com.aditya.worldcup.simulation.dto;

import com.aditya.worldcup.matches.dto.MatchResponse;

import java.util.List;

public record KnockoutSimulationResponse(
        Long tournamentId,
        String champion,
        List<MatchResponse> simulatedMatches,
        Integer completedMatches
) {
}
