package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.KnockoutSimulationResponse;
import com.aditya.worldcup.simulation.service.KnockoutSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class KnockoutSimulationController {

    private final KnockoutSimulationService knockoutSimulationService;

    @PostMapping("/{tournamentId}/knockout/simulate")
    public KnockoutSimulationResponse simulate(
            @PathVariable Long tournamentId
    ) {

        return knockoutSimulationService.simulate(tournamentId);
    }
}
