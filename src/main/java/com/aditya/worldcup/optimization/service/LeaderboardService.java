package com.aditya.worldcup.optimization.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.optimization.config.OptimizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final OptimizationProperties properties;

    // Cache holding leaderboard results. Key is the leaderboard type/name
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final Object value;
        final long timestamp;

        CacheEntry(Object value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getCachedOrCompute(String key, java.util.function.Supplier<T> computer) {
        CacheEntry entry = cache.get(key);
        long ttl = properties.getAnalyticsCacheDurationMs();
        if (entry != null && (System.currentTimeMillis() - entry.timestamp) < ttl) {
            return (T) entry.value;
        }
        T result = computer.get();
        cache.put(key, new CacheEntry(result));
        return result;
    }

    public void clearCache() {
        cache.clear();
    }

    public record PlayerStatEntry(Long playerId, String playerName, String teamName, long count) {}
    public record PlayerRatingEntry(Long playerId, String playerName, String teamName, double averageRating, long matches) {}
    public record TeamStatEntry(Long teamId, String teamName, double value) {}

    public List<PlayerStatEntry> getHighestScoringPlayers() {
        return getCachedOrCompute("highestScoringPlayers", () -> {
            List<Object[]> results = matchEventRepository.findTopScoringPlayers(PageRequest.of(0, 10));
            return mapToPlayerStatEntries(results);
        });
    }

    public List<PlayerStatEntry> getMostAssists() {
        return getCachedOrCompute("mostAssists", () -> {
            List<Object[]> results = matchEventRepository.findTopPlayersByEventType(MatchEventType.ASSIST, PageRequest.of(0, 10));
            return mapToPlayerStatEntries(results);
        });
    }

    public List<PlayerStatEntry> getMostYellowCards() {
        return getCachedOrCompute("mostYellowCards", () -> {
            List<Object[]> results = matchEventRepository.findTopPlayersByEventType(MatchEventType.YELLOW_CARD, PageRequest.of(0, 10));
            return mapToPlayerStatEntries(results);
        });
    }

    public List<PlayerStatEntry> getMostRedCards() {
        return getCachedOrCompute("mostRedCards", () -> {
            List<Object[]> results = matchEventRepository.findTopPlayersByEventType(MatchEventType.RED_CARD, PageRequest.of(0, 10));
            return mapToPlayerStatEntries(results);
        });
    }

    public List<PlayerRatingEntry> getHighestRatedPlayers() {
        return getCachedOrCompute("highestRatedPlayers", () ->
                playerMatchRatingRepository.findTopAverageRatedPlayers(PageRequest.of(0, 10))
                        .stream()
                        .map(row -> {
                            Player player = (Player) row[0];
                            double averageRating = ((Number) row[1]).doubleValue();
                            long matches = ((Number) row[2]).longValue();
                            return new PlayerRatingEntry(
                                    player.getId(),
                                    player.getName(),
                                    teamName(player),
                                    Math.round(averageRating * 100.0) / 100.0,
                                    matches
                            );
                        })
                        .toList());
    }

    public List<PlayerStatEntry> getTopCleanSheetPlayers() {
        return getCachedOrCompute("topCleanSheetPlayers", () -> {
            Map<Player, Long> cleanSheets = new HashMap<>();
            List<Match> completed = matchRepository.findCompletedMatchesHistory();
            for (Match match : completed) {
                if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
                    continue;
                }
                if (match.getAwayScore() != null && match.getAwayScore() == 0) {
                    addGoalkeeperCleanSheets(cleanSheets, match, match.getHomeTeam());
                }
                if (match.getHomeScore() != null && match.getHomeScore() == 0) {
                    addGoalkeeperCleanSheets(cleanSheets, match, match.getAwayTeam());
                }
            }
            return cleanSheets.entrySet()
                    .stream()
                    .sorted(Map.Entry.<Player, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> new PlayerStatEntry(
                            entry.getKey().getId(),
                            entry.getKey().getName(),
                            teamName(entry.getKey()),
                            entry.getValue()))
                    .toList();
        });
    }

    public List<TeamStatEntry> getHighestScoringTeams() {
        return getCachedOrCompute("highestScoringTeams", () -> {
            Map<Team, Integer> scoredMap = new HashMap<>();
            List<Match> completed = matchRepository.findCompletedMatchesHistory();
            for (Match m : completed) {
                if (m.getHomeTeam() != null && m.getHomeScore() != null) {
                    scoredMap.put(m.getHomeTeam(), scoredMap.getOrDefault(m.getHomeTeam(), 0) + m.getHomeScore());
                }
                if (m.getAwayTeam() != null && m.getAwayScore() != null) {
                    scoredMap.put(m.getAwayTeam(), scoredMap.getOrDefault(m.getAwayTeam(), 0) + m.getAwayScore());
                }
            }
            return scoredMap.entrySet().stream()
                    .map(e -> new TeamStatEntry(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                    .sorted(Comparator.comparingDouble(TeamStatEntry::value).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
        });
    }

    public List<TeamStatEntry> getMostCleanSheets() {
        return getCachedOrCompute("mostCleanSheets", () -> {
            Map<Team, Integer> cleanSheetsMap = new HashMap<>();
            List<Match> completed = matchRepository.findCompletedMatchesHistory();
            for (Match m : completed) {
                if (m.getHomeTeam() != null && m.getAwayScore() != null && m.getAwayScore() == 0) {
                    cleanSheetsMap.put(m.getHomeTeam(), cleanSheetsMap.getOrDefault(m.getHomeTeam(), 0) + 1);
                }
                if (m.getAwayTeam() != null && m.getHomeScore() != null && m.getHomeScore() == 0) {
                    cleanSheetsMap.put(m.getAwayTeam(), cleanSheetsMap.getOrDefault(m.getAwayTeam(), 0) + 1);
                }
            }
            return cleanSheetsMap.entrySet().stream()
                    .map(e -> new TeamStatEntry(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                    .sorted(Comparator.comparingDouble(TeamStatEntry::value).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
        });
    }

    public List<TeamStatEntry> getBestAttackingTeams() {
        return getCachedOrCompute("bestAttackingTeams", () -> {
            Map<Team, TeamPerformance> performanceMap = computeTeamPerformances();
            return performanceMap.entrySet().stream()
                    .filter(entry -> entry.getValue().matchesPlayed >= 3)
                    .map(entry -> {
                        Team t = entry.getKey();
                        TeamPerformance p = entry.getValue();
                        double rounded = Math.round(((double) p.goalsScored / p.matchesPlayed) * 100.0) / 100.0;
                        return new TeamStatEntry(t.getId(), t.getName(), rounded);
                    })
                    .sorted(Comparator.comparingDouble(TeamStatEntry::value).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
        });
    }

    public List<TeamStatEntry> getBestDefensiveTeams() {
        return getCachedOrCompute("bestDefensiveTeams", () -> {
            Map<Team, TeamPerformance> performanceMap = computeTeamPerformances();
            return performanceMap.entrySet().stream()
                    .filter(entry -> entry.getValue().matchesPlayed >= 3)
                    .map(entry -> {
                        Team t = entry.getKey();
                        TeamPerformance p = entry.getValue();
                        double rounded = Math.round(((double) p.goalsConceded / p.matchesPlayed) * 100.0) / 100.0;
                        return new TeamStatEntry(t.getId(), t.getName(), rounded);
                    })
                    .sorted(Comparator.comparingDouble(TeamStatEntry::value)) // Less is better!
                    .limit(10)
                    .collect(Collectors.toList());
        });
    }

    private static class TeamPerformance {
        int goalsScored = 0;
        int goalsConceded = 0;
        int matchesPlayed = 0;
    }

    private Map<Team, TeamPerformance> computeTeamPerformances() {
        Map<Team, TeamPerformance> performanceMap = new HashMap<>();
        List<Match> completed = matchRepository.findCompletedMatchesHistory();
        for (Match m : completed) {
            if (m.getHomeTeam() == null || m.getAwayTeam() == null) continue;
            
            TeamPerformance homePerf = performanceMap.computeIfAbsent(m.getHomeTeam(), k -> new TeamPerformance());
            TeamPerformance awayPerf = performanceMap.computeIfAbsent(m.getAwayTeam(), k -> new TeamPerformance());

            int homeScore = m.getHomeScore() != null ? m.getHomeScore() : 0;
            int awayScore = m.getAwayScore() != null ? m.getAwayScore() : 0;

            homePerf.goalsScored += homeScore;
            homePerf.goalsConceded += awayScore;
            homePerf.matchesPlayed++;

            awayPerf.goalsScored += awayScore;
            awayPerf.goalsConceded += homeScore;
            awayPerf.matchesPlayed++;
        }
        return performanceMap;
    }

    private List<PlayerStatEntry> mapToPlayerStatEntries(List<Object[]> results) {
        List<PlayerStatEntry> entries = new ArrayList<>();
        for (Object[] row : results) {
            Player player = (Player) row[0];
            long count = (Long) row[1];
            entries.add(new PlayerStatEntry(player.getId(), player.getName(), teamName(player), count));
        }
        return entries;
    }

    private void addGoalkeeperCleanSheets(Map<Player, Long> cleanSheets,
                                          Match match,
                                          Team team) {
        playerMatchRatingRepository.findByMatchId(match.getId())
                .stream()
                .map(rating -> rating.getPlayer())
                .filter(Objects::nonNull)
                .filter(player -> player.getPosition() == PlayerPosition.GK)
                .filter(player -> player.getCountry() != null
                        && team.getCountry() != null
                        && player.getCountry().getId().equals(team.getCountry().getId()))
                .forEach(player -> cleanSheets.put(player, cleanSheets.getOrDefault(player, 0L) + 1));
    }

    private String teamName(Player player) {
        return player.getCountry() == null ? "Unknown" : player.getCountry().getName();
    }
}
