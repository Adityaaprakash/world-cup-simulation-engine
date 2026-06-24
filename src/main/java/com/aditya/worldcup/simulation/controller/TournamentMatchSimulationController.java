package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.TournamentMatchSimulationResponse;
import com.aditya.worldcup.simulation.service.TournamentMatchSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentMatchSimulationController {

    private final TournamentMatchSimulationService
            tournamentMatchSimulationService;

    @PostMapping("/{tournamentId}/matches/{matchId}/simulate")
    public TournamentMatchSimulationResponse simulate(
            @PathVariable Long tournamentId,
            @PathVariable Long matchId
    ) {

        return tournamentMatchSimulationService.simulate(
                tournamentId,
                matchId
        );
    }
}