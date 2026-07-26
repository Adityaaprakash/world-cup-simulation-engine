package com.aditya.worldcup.matches.controller;

import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.dto.MatchResultRequest;
import com.aditya.worldcup.matches.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tournament Matches", description = "Tournament match lookup and manual completion")
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/{id}/matches")
    @Operation(summary = "List tournament matches", description = "Returns all matches for a tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matches returned"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public List<MatchResponse> getTournamentMatches(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return matchService.getTournamentMatches(id);
    }

    @PostMapping("/{id}/matches/{matchId}/complete")
    @Operation(summary = "Complete match manually", description = "Records a manual match result and updates standings where applicable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match completed"),
            @ApiResponse(responseCode = "400", description = "Invalid score or match mapping"),
            @ApiResponse(responseCode = "404", description = "Tournament or match not found"),
            @ApiResponse(responseCode = "409", description = "Match already completed")
    })
    public MatchResponse completeMatch(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id,
            @Parameter(description = "Match id")
            @PathVariable @Positive Long matchId,
            @Valid @RequestBody MatchResultRequest request
    ) {

        return matchService.completeMatch(
                id,
                matchId,
                request
        );
    }
}
