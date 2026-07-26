package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.MatchSimulationRequest;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.service.MatchSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@Tag(name = "Simulation", description = "Standalone match simulation")
public class SimulationController {

    private final MatchSimulationService matchSimulationService;

    @PostMapping("/match")
    @Operation(summary = "Simulate standalone match", description = "Simulates a match between two squads without changing endpoint semantics.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match simulated"),
            @ApiResponse(responseCode = "400", description = "Invalid simulation request")
    })
    public MatchSimulationResponse simulateMatch(
            @Valid @RequestBody MatchSimulationRequest request
    ) {

        return matchSimulationService.simulate(
                request
        );
    }
}
