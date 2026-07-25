package com.aditya.worldcup.optimization.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.optimization.config.OptimizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final MatchEventRepository matchEventRepository;
    private final SquadRepository squadRepository;
    private final OptimizationProperties properties;

    private static class CacheEntry {
        final Object value;
        final long timestamp;

        CacheEntry(Object value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

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

    public record FormationAnalytics(
            String formationName,
            long usageCount,
            long wins,
            long draws,
            long losses,
            double winPercentage,
            double avgGoalsScored,
            double avgGoalsConceded,
            double cleanSheetPercentage,
            double avgPossession,
            double avgShots,
            double avgCorners,
            double avgCards
    ) {}

    public record TournamentAverages(
            Long tournamentId,
            String tournamentName,
            long matchesCount,
            double avgGoals,
            double avgPossession,
            double avgShots,
            double avgCorners,
            double avgCards,
            double shootoutFrequency,
            double extraTimeFrequency
    ) {}

    public record SimulationAnalyticsReport(
            List<FormationAnalytics> formationAnalytics,
            List<TournamentAverages> tournamentAverages
    ) {}

    public SimulationAnalyticsReport getAnalyticsReport() {
        return getCachedOrCompute("analyticsReport", this::computeAnalyticsReport);
    }

    private SimulationAnalyticsReport computeAnalyticsReport() {
        long start = System.currentTimeMillis();
        List<Match> completedMatches = matchRepository.findCompletedMatchesHistory();
        if (completedMatches.isEmpty()) {
            log.info("Analytics generation completed: no completed matches available");
            return new SimulationAnalyticsReport(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> matchIds = completedMatches.stream().map(Match::getId).toList();

        // 1. Fetch match statistics
        List<MatchStatistics> statsList = matchStatisticsRepository.findByMatchIdIn(matchIds);
        Map<Long, MatchStatistics> statsMap = statsList.stream()
                .filter(s -> s.getMatch() != null)
                .collect(Collectors.toMap(s -> s.getMatch().getId(), s -> s, (s1, s2) -> s1));

        // 2. Fetch match events
        List<MatchEvent> eventsList = matchEventRepository.findByMatchIdIn(matchIds);
        Map<Long, List<MatchEvent>> eventsMap = eventsList.stream()
                .filter(e -> e.getMatch() != null)
                .collect(Collectors.groupingBy(e -> e.getMatch().getId()));

        // 3. Squad formation mapping
        List<Squad> squads = squadRepository.findAll();
        Map<Long, Formation> teamIdToFormation = squads.stream()
                .filter(s -> s.getTeam() != null && s.getFormation() != null)
                .collect(Collectors.toMap(s -> s.getTeam().getId(), Squad::getFormation, (f1, f2) -> f1));

        // Analytics counters
        Map<String, FormationCounters> formationCountersMap = new HashMap<>();
        Map<Long, TournamentCounters> tournamentCountersMap = new HashMap<>();

        for (Match match : completedMatches) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null) continue;
            Long matchId = match.getId();
            
            // Gather details
            int homeScore = match.getHomeScore() != null ? match.getHomeScore() : 0;
            int awayScore = match.getAwayScore() != null ? match.getAwayScore() : 0;
            
            MatchStatistics stat = statsMap.get(matchId);
            List<MatchEvent> events = eventsMap.getOrDefault(matchId, Collections.emptyList());

            Formation homeFormation = teamIdToFormation.get(match.getHomeTeam().getId());
            Formation awayFormation = teamIdToFormation.get(match.getAwayTeam().getId());

            int homeShots = 0;
            int awayShots = 0;
            int homeCorners = 0;
            int awayCorners = 0;
            double homePossession = 50.0;
            double awayPossession = 50.0;

            if (stat != null) {
                homeShots = stat.getHomeShots() != null ? stat.getHomeShots() : 0;
                awayShots = stat.getAwayShots() != null ? stat.getAwayShots() : 0;
                homeCorners = stat.getHomeCorners() != null ? stat.getHomeCorners() : 0;
                awayCorners = stat.getAwayCorners() != null ? stat.getAwayCorners() : 0;
                homePossession = stat.getHomePossession() != null ? stat.getHomePossession() : 50.0;
                awayPossession = stat.getAwayPossession() != null ? stat.getAwayPossession() : 50.0;
            }

            int homeCards = 0;
            int awayCards = 0;
            for (MatchEvent e : events) {
                if (e.getEventType() == MatchEventType.YELLOW_CARD || e.getEventType() == MatchEventType.RED_CARD) {
                    // Check team assignment from player country
                    if (e.getPlayer() != null && e.getPlayer().getCountry() != null) {
                        Long playerCountryId = e.getPlayer().getCountry().getId();
                        if (playerCountryId.equals(match.getHomeTeam().getCountry().getId())) {
                            homeCards++;
                        } else {
                            awayCards++;
                        }
                    }
                }
            }

            // A. Update formation analytics
            if (homeFormation != null) {
                FormationCounters c = formationCountersMap.computeIfAbsent(homeFormation.getName(), k -> new FormationCounters());
                c.usageCount++;
                c.goalsScored += homeScore;
                c.goalsConceded += awayScore;
                c.possessionSum += homePossession;
                c.shotsSum += homeShots;
                c.cornersSum += homeCorners;
                c.cardsSum += homeCards;
                if (homeScore > awayScore) {
                    c.wins++;
                } else if (homeScore == awayScore) {
                    c.draws++;
                } else {
                    c.losses++;
                }
                if (awayScore == 0) {
                    c.cleanSheets++;
                }
            }

            if (awayFormation != null) {
                FormationCounters c = formationCountersMap.computeIfAbsent(awayFormation.getName(), k -> new FormationCounters());
                c.usageCount++;
                c.goalsScored += awayScore;
                c.goalsConceded += homeScore;
                c.possessionSum += awayPossession;
                c.shotsSum += awayShots;
                c.cornersSum += awayCorners;
                c.cardsSum += awayCards;
                if (awayScore > homeScore) {
                    c.wins++;
                } else if (homeScore == awayScore) {
                    c.draws++;
                } else {
                    c.losses++;
                }
                if (homeScore == 0) {
                    c.cleanSheets++;
                }
            }

            // B. Update tournament averages
            if (match.getTournament() != null) {
                Tournament tournament = match.getTournament();
                TournamentCounters tc = tournamentCountersMap.computeIfAbsent(tournament.getId(), k -> new TournamentCounters(tournament.getName()));
                tc.matchesCount++;
                tc.goalsSum += (homeScore + awayScore);
                tc.possessionSum += (homePossession + awayPossession) / 2.0;
                tc.shotsSum += (homeShots + awayShots);
                tc.cornersSum += (homeCorners + awayCorners);
                tc.cardsSum += (homeCards + awayCards);

                // Shootout and extra time
                boolean isShootoutVal = isShootout(match, events);
                boolean isExtraTimeVal = isExtraTime(match, events, isShootoutVal);
                if (isShootoutVal) tc.shootoutsCount++;
                if (isExtraTimeVal) tc.extraTimeCount++;
            }
        }

        // Map counters to DTOs
        List<FormationAnalytics> fAnalytics = formationCountersMap.entrySet().stream()
                .map(e -> {
                    FormationCounters c = e.getValue();
                    double winPct = c.usageCount == 0 ? 0.0 : ((double) c.wins / c.usageCount) * 100.0;
                    double csPct = c.usageCount == 0 ? 0.0 : ((double) c.cleanSheets / c.usageCount) * 100.0;
                    return new FormationAnalytics(
                            e.getKey(),
                            c.usageCount,
                            c.wins, c.draws, c.losses,
                            Math.round(winPct * 100.0) / 100.0,
                            Math.round(((double) c.goalsScored / c.usageCount) * 100.0) / 100.0,
                            Math.round(((double) c.goalsConceded / c.usageCount) * 100.0) / 100.0,
                            Math.round(csPct * 100.0) / 100.0,
                            Math.round((c.possessionSum / c.usageCount) * 100.0) / 100.0,
                            Math.round(((double) c.shotsSum / c.usageCount) * 100.0) / 100.0,
                            Math.round(((double) c.cornersSum / c.usageCount) * 100.0) / 100.0,
                            Math.round(((double) c.cardsSum / c.usageCount) * 100.0) / 100.0
                    );
                })
                .sorted(Comparator.comparing(FormationAnalytics::usageCount).reversed())
                .collect(Collectors.toList());

        List<TournamentAverages> tAverages = tournamentCountersMap.entrySet().stream()
                .map(e -> {
                    TournamentCounters tc = e.getValue();
                    long mc = tc.matchesCount;
                    return new TournamentAverages(
                            e.getKey(),
                            tc.tournamentName,
                            mc,
                            Math.round(((double) tc.goalsSum / mc) * 100.0) / 100.0,
                            Math.round((tc.possessionSum / mc) * 100.0) / 100.0,
                            Math.round(((double) tc.shotsSum / mc) * 100.0) / 100.0,
                            Math.round(((double) tc.cornersSum / mc) * 100.0) / 100.0,
                            Math.round(((double) tc.cardsSum / mc) * 100.0) / 100.0,
                            Math.round(((double) tc.shootoutsCount / mc) * 100.0) / 100.0,
                            Math.round(((double) tc.extraTimeCount / mc) * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());

        SimulationAnalyticsReport report = new SimulationAnalyticsReport(fAnalytics, tAverages);
        log.info("Analytics generation completed: matches={}, formations={}, tournaments={}, durationMs={}",
                completedMatches.size(),
                fAnalytics.size(),
                tAverages.size(),
                System.currentTimeMillis() - start);
        return report;
    }

    private boolean isShootout(Match match, List<MatchEvent> events) {
        if (match.getRound() == null || match.getRound() == MatchRound.GROUP_STAGE) {
            return false;
        }
        long eventGoals = events.stream()
                .filter(e -> e.getEventType() == MatchEventType.GOAL || e.getEventType() == MatchEventType.PENALTY)
                .count();
        int dbGoals = (match.getHomeScore() != null ? match.getHomeScore() : 0) + 
                      (match.getAwayScore() != null ? match.getAwayScore() : 0);
        return dbGoals > eventGoals;
    }

    private boolean isExtraTime(Match match, List<MatchEvent> events, boolean isShootoutVal) {
        if (match.getRound() == null || match.getRound() == MatchRound.GROUP_STAGE) {
            return false;
        }
        boolean hasLateEvents = events.stream().anyMatch(e -> e.getMinute() != null && e.getMinute() > 90);
        return hasLateEvents || isShootoutVal;
    }

    private static class FormationCounters {
        long usageCount = 0;
        long wins = 0;
        long draws = 0;
        long losses = 0;
        int goalsScored = 0;
        int goalsConceded = 0;
        long cleanSheets = 0;
        double possessionSum = 0;
        long shotsSum = 0;
        long cornersSum = 0;
        long cardsSum = 0;
    }

    private static class TournamentCounters {
        final String tournamentName;
        long matchesCount = 0;
        int goalsSum = 0;
        double possessionSum = 0;
        long shotsSum = 0;
        long cornersSum = 0;
        long cardsSum = 0;
        long shootoutsCount = 0;
        long extraTimeCount = 0;

        TournamentCounters(String name) {
            this.tournamentName = name;
        }
    }
}
