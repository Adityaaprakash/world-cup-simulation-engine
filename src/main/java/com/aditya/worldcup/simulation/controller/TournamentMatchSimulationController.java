package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.TournamentMatchSimulationResponse;
import com.aditya.worldcup.simulation.service.TournamentMatchSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tournament Simulation", description = "Tournament simulation workflows")
public class TournamentMatchSimulationController {

    private final TournamentMatchSimulationService
            tournamentMatchSimulationService;

    @PostMapping("/{tournamentId}/matches/{matchId}/simulate")
    @Operation(summary = "Simulate tournament match", description = "Simulates one scheduled tournament match and updates dependent tournament state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament match simulated"),
            @ApiResponse(responseCode = "404", description = "Tournament or match not found"),
            @ApiResponse(responseCode = "409", description = "Match or tournament cannot be simulated in current state")
    })
    public TournamentMatchSimulationResponse simulate(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long tournamentId,
            @Parameter(description = "Match id")
            @PathVariable @Positive Long matchId
    ) {

        return tournamentMatchSimulationService.simulate(
                tournamentId,
                matchId
        );
    }
}
