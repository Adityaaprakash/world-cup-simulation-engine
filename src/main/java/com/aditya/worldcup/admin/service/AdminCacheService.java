package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.CacheOperationResponse;
import com.aditya.worldcup.optimization.service.AnalyticsService;
import com.aditya.worldcup.optimization.service.CachedTournamentDataService;
import com.aditya.worldcup.optimization.service.LeaderboardService;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminCacheService {

    private final AnalyticsService analyticsService;
    private final LeaderboardService leaderboardService;
    private final CachedTournamentDataService cachedTournamentDataService;
    private final TournamentRepository tournamentRepository;
    private final AdminAuditService adminAuditService;
    private final MaintenanceHistoryService maintenanceHistoryService;

    public CacheOperationResponse clear(String cache, Authentication authentication) {
        String target = normalize(cache);
        long started = System.currentTimeMillis();
        clearTarget(target);
        long duration = System.currentTimeMillis() - started;
        String details = "Cleared " + target.toLowerCase(Locale.ROOT) + " cache";
        record("CACHE_CLEAR", authentication, duration, details);
        return new CacheOperationResponse("CACHE_CLEAR", target, "SUCCESS", duration,
                details, LocalDateTime.now());
    }

    public CacheOperationResponse rebuild(String cache, Authentication authentication) {
        String target = normalize(cache);
        long started = System.currentTimeMillis();
        clearTarget(target);
        int rebuilt = rebuildTarget(target);
        long duration = System.currentTimeMillis() - started;
        String details = "Rebuilt " + target.toLowerCase(Locale.ROOT)
                + " cache entries=" + rebuilt;
        record("CACHE_REBUILD", authentication, duration, details);
        return new CacheOperationResponse("CACHE_REBUILD", target, "SUCCESS", duration,
                details, LocalDateTime.now());
    }

    private void clearTarget(String target) {
        if ("ALL".equals(target) || "ANALYTICS".equals(target)) analyticsService.clearCache();
        if ("ALL".equals(target) || "LEADERBOARD".equals(target)) leaderboardService.clearCache();
        if ("ALL".equals(target) || "TOURNAMENT".equals(target)) cachedTournamentDataService.clearCache();
    }

    private int rebuildTarget(String target) {
        int rebuilt = 0;
        if ("ALL".equals(target) || "ANALYTICS".equals(target)) {
            analyticsService.getAnalyticsReport();
            rebuilt++;
        }
        if ("ALL".equals(target) || "LEADERBOARD".equals(target)) {
            leaderboardService.getHighestScoringPlayers();
            leaderboardService.getMostAssists();
            leaderboardService.getMostYellowCards();
            leaderboardService.getMostRedCards();
            leaderboardService.getHighestRatedPlayers();
            leaderboardService.getTopCleanSheetPlayers();
            leaderboardService.getHighestScoringTeams();
            leaderboardService.getMostCleanSheets();
            leaderboardService.getBestAttackingTeams();
            leaderboardService.getBestDefensiveTeams();
            rebuilt += 10;
        }
        if ("ALL".equals(target) || "TOURNAMENT".equals(target)) {
            rebuilt += tournamentRepository.findAll().stream()
                    .filter(tournament -> tournament.getStatus() == TournamentStatus.COMPLETED
                            || tournament.getStatus() == TournamentStatus.ARCHIVED)
                    .mapToInt(tournament -> {
                        cachedTournamentDataService.getSummary(tournament.getId());
                        cachedTournamentDataService.getAwards(tournament.getId());
                        cachedTournamentDataService.getTeamAwards(tournament.getId());
                        return 3;
                    })
                    .sum();
        }
        return rebuilt;
    }

    private String normalize(String cache) {
        String target = cache == null || cache.isBlank()
                ? "ALL" : cache.trim().toUpperCase(Locale.ROOT);
        if (!target.equals("ALL") && !target.equals("ANALYTICS")
                && !target.equals("LEADERBOARD") && !target.equals("TOURNAMENT")) {
            throw new IllegalArgumentException("Unsupported cache: " + cache);
        }
        return target;
    }

    private void record(String operation, Authentication authentication, long duration, String details) {
        String username = authentication == null ? "unknown" : authentication.getName();
        adminAuditService.log(username, operation, "CACHE", 0L);
        maintenanceHistoryService.record(operation, username, duration, "SUCCESS", details);
    }
}
