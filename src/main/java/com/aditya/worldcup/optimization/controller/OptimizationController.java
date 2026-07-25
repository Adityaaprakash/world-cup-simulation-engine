package com.aditya.worldcup.optimization.controller;

import com.aditya.worldcup.optimization.service.AnalyticsService;
import com.aditya.worldcup.optimization.service.BenchmarkService;
import com.aditya.worldcup.optimization.service.CachedTournamentDataService;
import com.aditya.worldcup.optimization.service.LeaderboardService;
import com.aditya.worldcup.optimization.service.SimulationMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
public class OptimizationController {

    private final SimulationMetricsService metricsService;
    private final BenchmarkService benchmarkService;
    private final AnalyticsService analyticsService;
    private final LeaderboardService leaderboardService;
    private final CachedTournamentDataService cachedTournamentDataService;

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return metricsService.getAggregatedStatistics();
    }

    @PostMapping("/benchmark")
    public BenchmarkService.BenchmarkReport runBenchmark(
            @RequestParam(required = false) Integer matches,
            @RequestParam(required = false) Integer tournaments) {
        return benchmarkService.runBenchmark(matches, tournaments);
    }

    @GetMapping("/analytics")
    public AnalyticsService.SimulationAnalyticsReport getAnalytics() {
        return analyticsService.getAnalyticsReport();
    }

    @GetMapping("/leaderboards/scorers")
    public List<LeaderboardService.PlayerStatEntry> getHighestScoringPlayers() {
        return leaderboardService.getHighestScoringPlayers();
    }

    @GetMapping("/leaderboards/assists")
    public List<LeaderboardService.PlayerStatEntry> getMostAssists() {
        return leaderboardService.getMostAssists();
    }

    @GetMapping("/leaderboards/cards/yellow")
    public List<LeaderboardService.PlayerStatEntry> getMostYellowCards() {
        return leaderboardService.getMostYellowCards();
    }

    @GetMapping("/leaderboards/cards/red")
    public List<LeaderboardService.PlayerStatEntry> getMostRedCards() {
        return leaderboardService.getMostRedCards();
    }

    @GetMapping("/leaderboards/ratings")
    public List<LeaderboardService.PlayerRatingEntry> getHighestRatedPlayers() {
        return leaderboardService.getHighestRatedPlayers();
    }

    @GetMapping("/leaderboards/cleansheets")
    public List<LeaderboardService.PlayerStatEntry> getTopCleanSheetPlayers() {
        return leaderboardService.getTopCleanSheetPlayers();
    }

    @GetMapping("/leaderboards/teams/scorers")
    public List<LeaderboardService.TeamStatEntry> getHighestScoringTeams() {
        return leaderboardService.getHighestScoringTeams();
    }

    @GetMapping("/leaderboards/teams/cleansheets")
    public List<LeaderboardService.TeamStatEntry> getMostCleanSheets() {
        return leaderboardService.getMostCleanSheets();
    }

    @GetMapping("/leaderboards/teams/attacking")
    public List<LeaderboardService.TeamStatEntry> getBestAttackingTeams() {
        return leaderboardService.getBestAttackingTeams();
    }

    @GetMapping("/leaderboards/teams/defensive")
    public List<LeaderboardService.TeamStatEntry> getBestDefensiveTeams() {
        return leaderboardService.getBestDefensiveTeams();
    }

    @PostMapping("/cache/clear")
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
