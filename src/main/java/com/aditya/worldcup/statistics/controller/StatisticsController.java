package com.aditya.worldcup.statistics.controller;

import com.aditya.worldcup.statistics.dto.*;
import com.aditya.worldcup.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Advanced Statistics Platform", description = "Endpoints for player, team, tournament, match stats and football achievements.")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/players")
    @Operation(summary = "Get player career and skill stats page", description = "Supports paging, sorting, and filters by name, position, or country.")
    @ApiResponse(responseCode = "200", description = "Player stats retrieved successfully")
    public Page<PlayerStatisticsResponse> getPlayers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String country,
            Pageable pageable) {
        return statisticsService.searchPlayers(name, position, country, pageable);
    }

    @GetMapping("/teams")
    @Operation(summary = "Get team tournament analytics page", description = "Supports paging, sorting, and filters by name.")
    @ApiResponse(responseCode = "200", description = "Team stats retrieved successfully")
    public Page<TeamStatisticsResponse> getTeams(
            @RequestParam(required = false) String name,
            Pageable pageable) {
        return statisticsService.searchTeams(name, pageable);
    }

    @GetMapping("/tournaments")
    @Operation(summary = "Get tournament statistics page", description = "Supports paging, sorting, and filters by name, year, or status.")
    @ApiResponse(responseCode = "200", description = "Tournament stats retrieved successfully")
    public Page<TournamentStatisticsResponse> getTournaments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return statisticsService.searchTournaments(name, year, status, pageable);
    }

    @GetMapping("/matches")
    @Operation(summary = "Get finished match statistics page", description = "Supports paging, sorting, and filters by tournamentId, round, teamId, or status.")
    @ApiResponse(responseCode = "200", description = "Match stats retrieved successfully")
    public Page<MatchStatisticsResponse> getMatches(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String round,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return statisticsService.searchMatches(tournamentId, round, teamId, status, pageable);
    }

    @GetMapping("/records")
    @Operation(summary = "Get all-time football achievements and peak records", description = "Returns player, team, and match records registry.")
    @ApiResponse(responseCode = "200", description = "Records registry retrieved successfully")
    public FootballRecordsResponse getRecords() {
        return statisticsService.getRecords();
    }

    @GetMapping("/summary")
    @Operation(summary = "Get a high-level summary of the simulation engine", description = "Returns counters and global performance ratios.")
    @ApiResponse(responseCode = "200", description = "Global summary retrieved successfully")
    public StatisticsSummaryResponse getSummary() {
        return statisticsService.getSummary();
    }
}
