package com.aditya.worldcup.matches.controller;

import com.aditya.worldcup.matches.dto.MatchDetailResponse;
import com.aditya.worldcup.matches.dto.MatchHistoryResponse;
import com.aditya.worldcup.matches.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Validated
@Tag(name = "Matches", description = "General match history and details")
public class GeneralMatchController {

    private final MatchService matchService;

    @GetMapping("/history")
    @Operation(summary = "List completed matches", description = "Returns completed match history across tournaments.")
    @ApiResponse(responseCode = "200", description = "Match history returned")
    public List<MatchHistoryResponse> getMatchHistory() {
        return matchService.getMatchHistory();
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Get match detail", description = "Returns detailed match data including events, statistics, ratings, and commentary.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Match detail returned"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public MatchDetailResponse getMatchDetail(
            @Parameter(description = "Match id")
            @PathVariable @Positive Long matchId
    ) {
        return matchService.getMatchDetail(matchId);
    }
}
