package com.aditya.worldcup.optimization.service;

import com.aditya.worldcup.optimization.config.OptimizationProperties;
import com.aditya.worldcup.tournaments.dto.TournamentAwardsResponse;
import com.aditya.worldcup.tournaments.dto.TournamentSummaryResponse;
import com.aditya.worldcup.tournaments.dto.TournamentTeamAwardsResponse;
import com.aditya.worldcup.tournaments.service.TournamentAwardsService;
import com.aditya.worldcup.tournaments.service.TournamentSummaryService;
import com.aditya.worldcup.tournaments.service.TournamentTeamAwardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CachedTournamentDataService {

    private final TournamentAwardsService tournamentAwardsService;
    private final TournamentSummaryService tournamentSummaryService;
    private final TournamentTeamAwardsService tournamentTeamAwardsService;
    private final OptimizationProperties properties;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TournamentAwardsResponse getAwards(Long tournamentId) {
        return getCachedOrCompute("awards:" + tournamentId,
                () -> tournamentAwardsService.calculateAwards(tournamentId));
    }

    public TournamentSummaryResponse getSummary(Long tournamentId) {
        return getCachedOrCompute("summary:" + tournamentId,
                () -> tournamentSummaryService.summarize(tournamentId));
    }

    public TournamentTeamAwardsResponse getTeamAwards(Long tournamentId) {
        return getCachedOrCompute("team-awards:" + tournamentId,
                () -> tournamentTeamAwardsService.calculate(tournamentId));
    }

    public void clearCache() {
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> T getCachedOrCompute(String key, Supplier<T> supplier) {
        CacheEntry entry = cache.get(key);
        long ttl = properties.getCacheDurationMs();
        if (entry != null && System.currentTimeMillis() - entry.timestamp < ttl) {
            return (T) entry.value;
        }
        T value = supplier.get();
        cache.put(key, new CacheEntry(value));
        return value;
    }

    private record CacheEntry(Object value, long timestamp) {

        private CacheEntry(Object value) {
            this(value, System.currentTimeMillis());
        }
    }
}
