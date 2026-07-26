package com.aditya.worldcup.tournaments.controller;

import com.aditya.worldcup.optimization.service.CachedTournamentDataService;
import com.aditya.worldcup.tournaments.dto.TournamentAwardsResponse;
import com.aditya.worldcup.tournaments.dto.TournamentSummaryResponse;
import com.aditya.worldcup.tournaments.dto.TournamentTeamAwardsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tournament Reports", description = "Awards, team awards, and tournament summary reports")
public class TournamentAwardsController {

    private final CachedTournamentDataService cachedTournamentDataService;

    @GetMapping("/{tournamentId}/awards")
    @Operation(summary = "Get tournament awards", description = "Returns individual tournament awards.")
    @ApiResponse(responseCode = "200", description = "Tournament awards returned")
    public TournamentAwardsResponse getAwards(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long tournamentId
    ) {

        return cachedTournamentDataService.getAwards(tournamentId);
    }

    @GetMapping("/{tournamentId}/team-awards")
    @Operation(summary = "Get team awards", description = "Returns team-level tournament awards.")
    @ApiResponse(responseCode = "200", description = "Team awards returned")
    public TournamentTeamAwardsResponse getTeamAwards(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long tournamentId
    ) {

        return cachedTournamentDataService.getTeamAwards(tournamentId);
    }

    @GetMapping("/{tournamentId}/summary")
    @Operation(summary = "Get tournament summary", description = "Returns aggregate tournament summary and narrative data.")
    @ApiResponse(responseCode = "200", description = "Tournament summary returned")
    public TournamentSummaryResponse getSummary(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long tournamentId
    ) {

        return cachedTournamentDataService.getSummary(tournamentId);
    }
}
