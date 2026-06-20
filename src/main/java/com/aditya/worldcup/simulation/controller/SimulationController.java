package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.MatchSimulationRequest;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.service.MatchSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final MatchSimulationService matchSimulationService;

    @PostMapping("/match")
    public MatchSimulationResponse simulateMatch(
            @RequestBody MatchSimulationRequest request
    ) {

        return matchSimulationService.simulate(
                request
        );
    }
}