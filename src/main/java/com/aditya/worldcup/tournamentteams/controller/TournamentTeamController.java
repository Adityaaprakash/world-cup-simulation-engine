package com.aditya.worldcup.tournamentteams.controller;

import com.aditya.worldcup.tournamentteams.dto.RegisterTeamRequest;
import com.aditya.worldcup.tournamentteams.dto.TournamentTeamResponse;
import com.aditya.worldcup.tournamentteams.service.TournamentTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tournament Teams", description = "Tournament team registration")
public class TournamentTeamController {

    private final TournamentTeamService tournamentTeamService;

    @PostMapping("/{id}/register")
    @Operation(summary = "Register team", description = "Registers a national team into an upcoming tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Team registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration payload"),
            @ApiResponse(responseCode = "404", description = "Tournament or team not found"),
            @ApiResponse(responseCode = "409", description = "Registration closed or team already registered")
    })
    public ResponseEntity<Void> registerTeam(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id,
            @Valid @RequestBody RegisterTeamRequest request) {

        tournamentTeamService.registerTeam(id, request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/tournaments/{id}/teams")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}/teams")
    @Operation(summary = "List registered teams", description = "Returns teams registered in a tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registered teams returned"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public List<TournamentTeamResponse> getTournamentTeams(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return tournamentTeamService.getTournamentTeams(id);
    }
}
