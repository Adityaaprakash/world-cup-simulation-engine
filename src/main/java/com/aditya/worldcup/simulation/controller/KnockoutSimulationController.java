package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.KnockoutSimulationResponse;
import com.aditya.worldcup.simulation.service.KnockoutSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tournament Simulation", description = "Tournament simulation workflows")
public class KnockoutSimulationController {

    private final KnockoutSimulationService knockoutSimulationService;

    @PostMapping("/{tournamentId}/knockout/simulate")
    @Operation(summary = "Simulate knockout stage", description = "Simulates knockout rounds and completes the tournament champion path.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Knockout stage simulated"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Knockout cannot be simulated in current state")
    })
    public KnockoutSimulationResponse simulate(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long tournamentId
    ) {

        return knockoutSimulationService.simulate(tournamentId);
    }
}
