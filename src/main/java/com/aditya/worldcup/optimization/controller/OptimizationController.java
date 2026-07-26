package com.aditya.worldcup.optimization.controller;

import com.aditya.worldcup.optimization.service.AnalyticsService;
import com.aditya.worldcup.optimization.service.BenchmarkService;
import com.aditya.worldcup.optimization.service.CachedTournamentDataService;
import com.aditya.worldcup.optimization.service.LeaderboardService;
import com.aditya.worldcup.optimization.service.SimulationMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
@Validated
@Tag(name = "Optimization", description = "Metrics, benchmark, analytics, leaderboards, and cache operations")
public class OptimizationController {

    private final SimulationMetricsService metricsService;
    private final BenchmarkService benchmarkService;
    private final AnalyticsService analyticsService;
    private final LeaderboardService leaderboardService;
    private final CachedTournamentDataService cachedTournamentDataService;

    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get simulation metrics", description = "Returns aggregated execution metrics.")
    @ApiResponse(responseCode = "200", description = "Metrics returned")
    public Map<String, Object> getMetrics() {
        return metricsService.getAggregatedStatistics();
    }

    @PostMapping("/benchmark")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run benchmark", description = "Runs an optional bounded benchmark for matches and tournaments.")
    @ApiResponse(responseCode = "200", description = "Benchmark report returned")
    public BenchmarkService.BenchmarkReport runBenchmark(
            @Parameter(description = "Number of standalone matches to simulate")
            @RequestParam(required = false) @PositiveOrZero Integer matches,
            @Parameter(description = "Number of full tournaments to simulate")
            @RequestParam(required = false) @PositiveOrZero Integer tournaments) {
        return benchmarkService.runBenchmark(matches, tournaments);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get analytics report", description = "Returns simulation analytics summaries.")
    @ApiResponse(responseCode = "200", description = "Analytics returned")
    public AnalyticsService.SimulationAnalyticsReport getAnalytics() {
        return analyticsService.getAnalyticsReport();
    }

    @GetMapping("/leaderboards/scorers")
    @Operation(summary = "Get top scorers", description = "Returns the highest-scoring players.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.PlayerStatEntry> getHighestScoringPlayers() {
        return leaderboardService.getHighestScoringPlayers();
    }

    @GetMapping("/leaderboards/assists")
    @Operation(summary = "Get top assist providers", description = "Returns players with the most assists.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.PlayerStatEntry> getMostAssists() {
        return leaderboardService.getMostAssists();
    }

    @GetMapping("/leaderboards/cards/yellow")
    @Operation(summary = "Get yellow-card leaderboard", description = "Returns players with the most yellow cards.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.PlayerStatEntry> getMostYellowCards() {
        return leaderboardService.getMostYellowCards();
    }

    @GetMapping("/leaderboards/cards/red")
    @Operation(summary = "Get red-card leaderboard", description = "Returns players with the most red cards.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.PlayerStatEntry> getMostRedCards() {
        return leaderboardService.getMostRedCards();
    }

    @GetMapping("/leaderboards/ratings")
    @Operation(summary = "Get highest-rated players", description = "Returns players with the best average match ratings.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.PlayerRatingEntry> getHighestRatedPlayers() {
        return leaderboardService.getHighestRatedPlayers();
    }

    @GetMapping("/leaderboards/cleansheets")
    @Operation(summary = "Get clean-sheet players", description = "Returns players associated with the most clean sheets.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.PlayerStatEntry> getTopCleanSheetPlayers() {
        return leaderboardService.getTopCleanSheetPlayers();
    }

    @GetMapping("/leaderboards/teams/scorers")
    @Operation(summary = "Get team scoring leaderboard", description = "Returns highest-scoring teams.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.TeamStatEntry> getHighestScoringTeams() {
        return leaderboardService.getHighestScoringTeams();
    }

    @GetMapping("/leaderboards/teams/cleansheets")
    @Operation(summary = "Get team clean-sheet leaderboard", description = "Returns teams with the most clean sheets.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.TeamStatEntry> getMostCleanSheets() {
        return leaderboardService.getMostCleanSheets();
    }

    @GetMapping("/leaderboards/teams/attacking")
    @Operation(summary = "Get attacking team leaderboard", description = "Returns teams ranked by attacking output.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.TeamStatEntry> getBestAttackingTeams() {
        return leaderboardService.getBestAttackingTeams();
    }

    @GetMapping("/leaderboards/teams/defensive")
    @Operation(summary = "Get defensive team leaderboard", description = "Returns teams ranked by defensive output.")
    @ApiResponse(responseCode = "200", description = "Leaderboard returned")
    public List<LeaderboardService.TeamStatEntry> getBestDefensiveTeams() {
        return leaderboardService.getBestDefensiveTeams();
    }

    @PostMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Clear analytics caches", description = "Clears optimization, analytics, leaderboard, and cached tournament data.")
    @ApiResponse(responseCode = "200", description = "Caches cleared")
    public Map<String, String> clearCache() {
        analyticsService.clearCache();
        leaderboardService.clearCache();
        cachedTournamentDataService.clearCache();
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("message", "Caches cleared successfully");
        return resp;
    }
}
