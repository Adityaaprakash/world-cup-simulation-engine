package com.aditya.worldcup.simulation.dto;

import com.aditya.worldcup.standings.dto.GroupStandingsResponse;

import java.util.List;

public record GroupStageSimulationResponse(
        Long tournamentId,
        Integer simulatedMatches,
        Integer completedMatches,
        List<GroupStandingsResponse> finalStandings
) {
}
