package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.GroupStageSimulationResponse;
import com.aditya.worldcup.simulation.service.GroupStageSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class GroupStageSimulationController {

    private final GroupStageSimulationService groupStageSimulationService;

    @PostMapping("/{tournamentId}/groups/simulate")
    public GroupStageSimulationResponse simulate(
            @PathVariable Long tournamentId
    ) {

        return groupStageSimulationService.simulate(tournamentId);
    }
}
