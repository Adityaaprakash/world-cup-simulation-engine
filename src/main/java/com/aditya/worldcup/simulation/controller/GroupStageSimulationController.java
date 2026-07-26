package com.aditya.worldcup.simulation.controller;

import com.aditya.worldcup.simulation.dto.GroupStageSimulationResponse;
import com.aditya.worldcup.simulation.service.GroupStageSimulationService;
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
public class GroupStageSimulationController {

    private final GroupStageSimulationService groupStageSimulationService;

    @PostMapping("/{tournamentId}/groups/simulate")
    @Operation(summary = "Simulate group stage", description = "Simulates all remaining group-stage matches for a tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group stage simulated"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Group stage cannot be simulated in current state")
    })
    public GroupStageSimulationResponse simulate(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long tournamentId
    ) {

        return groupStageSimulationService.simulate(tournamentId);
    }
}
