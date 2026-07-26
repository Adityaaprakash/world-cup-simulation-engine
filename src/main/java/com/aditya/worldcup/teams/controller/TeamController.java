package com.aditya.worldcup.teams.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.teams.dto.TeamResponse;
import com.aditya.worldcup.teams.service.TeamService;
import com.aditya.worldcup.matches.dto.MatchHistoryResponse;
import com.aditya.worldcup.matches.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Validated
@Tag(name = "Teams", description = "National team catalog and history")
public class TeamController {

    private final TeamService teamService;
    private final MatchService matchService;

    @GetMapping
    @Operation(summary = "List teams", description = "Returns all teams using the legacy list response.")
    @ApiResponse(responseCode = "200", description = "Teams returned")
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/page")
    @Operation(summary = "List teams with pagination", description = "Returns teams as a pageable response with optional sorting.")
    @ApiResponse(responseCode = "200", description = "Team page returned")
    public Page<TeamResponse> getTeamPage(
            @Parameter(description = "Pagination and sorting options")
            Pageable pageable) {
        return teamService.getTeamPage(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team", description = "Returns a team by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team returned"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public TeamResponse getTeam(
            @Parameter(description = "Team id")
            @PathVariable @Positive Long id
    ) {
        return teamService.getTeam(id);
    }

    @GetMapping("/{id}/players")
    @Operation(summary = "List team players", description = "Returns players available for a team.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Players returned"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public List<PlayerResponse> getTeamPlayers(
            @Parameter(description = "Team id")
            @PathVariable @Positive Long id
    ) {
        return teamService.getTeamPlayers(id);
    }

    @GetMapping("/{id}/matches")
    @Operation(summary = "List completed team matches", description = "Returns completed match history for a team.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match history returned"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public List<MatchHistoryResponse> getTeamMatches(
            @Parameter(description = "Team id")
            @PathVariable @Positive Long id
    ) {
        return matchService.getTeamMatchHistory(id);
    }
}
