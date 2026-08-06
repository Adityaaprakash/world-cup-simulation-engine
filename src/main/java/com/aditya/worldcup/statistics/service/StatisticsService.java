package com.aditya.worldcup.statistics.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.entity.MatchEvent;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.optimization.config.OptimizationProperties;
import com.aditya.worldcup.optimization.service.AnalyticsService;
import com.aditya.worldcup.optimization.service.LeaderboardService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournaments.service.TournamentAwardsService;
import com.aditya.worldcup.tournaments.service.TournamentSummaryService;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.statistics.dto.*;
import com.aditya.worldcup.statistics.dto.FootballRecordsResponse.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final SquadRepository squadRepository;
    private final TournamentAwardsService tournamentAwardsService;
    private final TournamentSummaryService tournamentSummaryService;
    private final AnalyticsService analyticsService;
    private final LeaderboardService leaderboardService;
    private final ManagerRepository managerRepository;
    private final OptimizationProperties properties;

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
        analyticsService.clearCache();
        leaderboardService.clearCache();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<PlayerStatisticsResponse> searchPlayers(String name, String position, String country, Pageable pageable) {
        List<PlayerStatisticsResponse> allStats = getCachedOrCompute("allPlayerStats", this::computeAllPlayerStats);
        Stream<PlayerStatisticsResponse> stream = allStats.stream();
        if (name != null && !name.isBlank()) {
            String lower = name.toLowerCase();
            stream = stream.filter(p -> p.getName().toLowerCase().contains(lower));
        }
        if (position != null && !position.isBlank()) {
            stream = stream.filter(p -> p.getPosition().equalsIgnoreCase(position));
        }
        if (country != null && !country.isBlank()) {
            String lower = country.toLowerCase();
            stream = stream.filter(p -> p.getCountry().toLowerCase().contains(lower));
        }
        List<PlayerStatisticsResponse> filtered = stream.collect(Collectors.toList());
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String prop = order.getProperty();
            boolean desc = order.getDirection().isDescending();
            Comparator<PlayerStatisticsResponse> comp = getPlayerComparator(prop);
            if (desc) comp = comp.reversed();
            filtered.sort(comp);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<PlayerStatisticsResponse> content = (start <= filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, filtered.size());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<TeamStatisticsResponse> searchTeams(String name, Pageable pageable) {
        List<TeamStatisticsResponse> allStats = getCachedOrCompute("allTeamStats", this::computeAllTeamStats);
        Stream<TeamStatisticsResponse> stream = allStats.stream();
        if (name != null && !name.isBlank()) {
            String lower = name.toLowerCase();
            stream = stream.filter(t -> t.getTeamName().toLowerCase().contains(lower));
        }
        List<TeamStatisticsResponse> filtered = stream.collect(Collectors.toList());
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String prop = order.getProperty();
            boolean desc = order.getDirection().isDescending();
            Comparator<TeamStatisticsResponse> comp = getTeamComparator(prop);
            if (desc) comp = comp.reversed();
            filtered.sort(comp);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<TeamStatisticsResponse> content = (start <= filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, filtered.size());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<TournamentStatisticsResponse> searchTournaments(String name, Integer year, String status, Pageable pageable) {
        List<TournamentStatisticsResponse> allStats = getCachedOrCompute("allTournamentStats", this::computeAllTournamentStats);
        Stream<TournamentStatisticsResponse> stream = allStats.stream();
        if (name != null && !name.isBlank()) {
            String lower = name.toLowerCase();
            stream = stream.filter(t -> t.getTournamentName().toLowerCase().contains(lower));
        }
        if (year != null) {
            stream = stream.filter(t -> t.getYear() == year);
        }
        if (status != null && !status.isBlank()) {
            stream = stream.filter(t -> t.getStatus().equalsIgnoreCase(status));
        }
        List<TournamentStatisticsResponse> filtered = stream.collect(Collectors.toList());
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String prop = order.getProperty();
            boolean desc = order.getDirection().isDescending();
            Comparator<TournamentStatisticsResponse> comp = getTournamentComparator(prop);
            if (desc) comp = comp.reversed();
            filtered.sort(comp);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<TournamentStatisticsResponse> content = (start <= filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, filtered.size());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<MatchStatisticsResponse> searchMatches(Long tournamentId, String round, Long teamId, String status, Pageable pageable) {
        List<MatchStatisticsResponse> allStats = getCachedOrCompute("allMatchStats", this::computeAllMatchStats);
        Stream<MatchStatisticsResponse> stream = allStats.stream();
        if (tournamentId != null) {
            stream = stream.filter(m -> tournamentId.equals(m.getTournamentId()));
        }
        if (round != null && !round.isBlank()) {
            stream = stream.filter(m -> round.equalsIgnoreCase(m.getRound()));
        }
        if (teamId != null) {
            stream = stream.filter(m -> teamId.equals(m.getHomeTeamId()) || teamId.equals(m.getAwayTeamId()));
        }
        if (status != null && !status.isBlank()) {
            stream = stream.filter(m -> status.equalsIgnoreCase(m.getStatus()));
        }
        List<MatchStatisticsResponse> filtered = stream.collect(Collectors.toList());
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String prop = order.getProperty();
            boolean desc = order.getDirection().isDescending();
            Comparator<MatchStatisticsResponse> comp = getMatchComparator(prop);
            if (desc) comp = comp.reversed();
            filtered.sort(comp);
        }
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<MatchStatisticsResponse> content = (start <= filtered.size()) ? filtered.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(content, pageable, filtered.size());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public FootballRecordsResponse getRecords() {
        return getCachedOrCompute("footballRecords", this::computeRecords);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public StatisticsSummaryResponse getSummary() {
        return getCachedOrCompute("statisticsSummary", this::computeSummary);
    }

    private List<PlayerStatisticsResponse> computeAllPlayerStats() {
        List<Player> allPlayers = playerRepository.findAll();
        List<PlayerMatchRating> allRatings = playerMatchRatingRepository.findAll();
        List<MatchEvent> allEvents = matchEventRepository.findAll();
        List<Match> allMatches = matchRepository.findAll();
        List<MatchStatistics> allMatchStats = matchStatisticsRepository.findAll();

        Map<Long, List<PlayerMatchRating>> ratingsByPlayer = allRatings.stream()
                .filter(r -> r.getPlayer() != null)
                .collect(Collectors.groupingBy(r -> r.getPlayer().getId()));
        Map<Long, List<MatchEvent>> eventsByPlayer = allEvents.stream()
                .filter(e -> e.getPlayer() != null)
                .collect(Collectors.groupingBy(e -> e.getPlayer().getId()));
        Map<Long, Match> matchesById = allMatches.stream().collect(Collectors.toMap(Match::getId, m -> m));
        Map<Long, MatchStatistics> matchStatsById = allMatchStats.stream().collect(Collectors.toMap(ms -> ms.getMatch().getId(), ms -> ms, (a, b) -> a));

        Map<Long, Long> motmAwards = allMatches.stream()
                .filter(m -> m.getManOfTheMatch() != null)
                .collect(Collectors.groupingBy(m -> m.getManOfTheMatch().getId(), Collectors.counting()));

        List<PlayerStatisticsResponse> stats = new ArrayList<>();
        for (Player p : allPlayers) {
            List<PlayerMatchRating> pRatings = ratingsByPlayer.getOrDefault(p.getId(), Collections.emptyList());
            List<MatchEvent> pEvents = eventsByPlayer.getOrDefault(p.getId(), Collections.emptyList());

            int appearances = pRatings.size();
            int goals = 0;
            int assists = 0;
            int cleanSheets = 0;
            int yellowCards = 0;
            int redCards = 0;
            int ownGoals = 0;
            int penaltiesScored = 0;
            int penaltiesMissed = 0;

            for (MatchEvent e : pEvents) {
                if (e.getEventType() == MatchEventType.GOAL) {
                    goals++;
                } else if (e.getEventType() == MatchEventType.ASSIST) {
                    assists++;
                } else if (e.getEventType() == MatchEventType.YELLOW_CARD) {
                    yellowCards++;
                } else if (e.getEventType() == MatchEventType.RED_CARD) {
                    redCards++;
                } else if (e.getEventType() == MatchEventType.OWN_GOAL) {
                    ownGoals++;
                } else if (e.getEventType() == MatchEventType.PENALTY) {
                    if (e.getDescription() != null && e.getDescription().toLowerCase().contains("converts")) {
                        penaltiesScored++;
                        goals++;
                    } else {
                        penaltiesMissed++;
                    }
                }
            }

            for (PlayerMatchRating r : pRatings) {
                Match m = matchesById.get(r.getMatch().getId());
                if (m != null && m.getStatus() == MatchStatus.FINISHED) {
                    if (p.getCountry() != null) {
                        Long pCountryId = p.getCountry().getId();
                        if (m.getHomeTeam() != null && m.getHomeTeam().getCountry() != null && pCountryId.equals(m.getHomeTeam().getCountry().getId())) {
                            if (m.getAwayScore() != null && m.getAwayScore() == 0) cleanSheets++;
                        } else if (m.getAwayTeam() != null && m.getAwayTeam().getCountry() != null && pCountryId.equals(m.getAwayTeam().getCountry().getId())) {
                            if (m.getHomeScore() != null && m.getHomeScore() == 0) cleanSheets++;
                        }
                    }
                }
            }

            double totalRating = 0.0;
            int totalMins = 0;
            double totalShots = 0.0;
            double totalShotsOnTarget = 0.0;
            double totalPassAccuracy = 0.0;
            double totalKeyPasses = 0.0;
            double totalTackles = 0.0;
            double totalInterceptions = 0.0;
            double totalBlocks = 0.0;
            int totalSaves = 0;
            int totalShotsFaced = 0;

            for (PlayerMatchRating r : pRatings) {
                Match m = matchesById.get(r.getMatch().getId());
                if (m == null) continue;
                totalRating += r.getRating();
                List<MatchEvent> mEvents = allEvents.stream().filter(e -> e.getMatch() != null && e.getMatch().getId().equals(m.getId())).toList();
                int mins = calculateMinutes(m, p, mEvents);
                totalMins += mins;
                double ratingVal = r.getRating();
                double minsFactor = mins / 90.0;

                String pos = p.getPosition() != null ? p.getPosition().name() : "CM";
                boolean isGk = "GK".equalsIgnoreCase(pos);
                boolean isDef = "CB".equalsIgnoreCase(pos) || "LB".equalsIgnoreCase(pos) || "RB".equalsIgnoreCase(pos);
                boolean isMid = "CM".equalsIgnoreCase(pos) || "LM".equalsIgnoreCase(pos) || "RM".equalsIgnoreCase(pos) || "CDM".equalsIgnoreCase(pos) || "CAM".equalsIgnoreCase(pos);
                boolean isAtt = "ST".equalsIgnoreCase(pos) || "CF".equalsIgnoreCase(pos) || "LW".equalsIgnoreCase(pos) || "RW".equalsIgnoreCase(pos);

                if (isGk) {
                    MatchStatistics ms = matchStatsById.get(m.getId());
                    int saves = 0;
                    int oppGoals = 0;
                    if (p.getCountry() != null && m.getHomeTeam() != null && m.getHomeTeam().getCountry() != null && p.getCountry().getId().equals(m.getHomeTeam().getCountry().getId())) {
                        saves = ms != null && ms.getHomeSaves() != null ? ms.getHomeSaves() : 0;
                        oppGoals = m.getAwayScore() != null ? m.getAwayScore() : 0;
                    } else {
                        saves = ms != null && ms.getAwaySaves() != null ? ms.getAwaySaves() : 0;
                        oppGoals = m.getHomeScore() != null ? m.getHomeScore() : 0;
                    }
                    totalSaves += saves;
                    totalShotsFaced += (saves + oppGoals);
                } else {
                    double baseShots = 0.0;
                    if (isAtt) baseShots = 2.5 + (p.getShooting() - 50) * 0.05 + (ratingVal - 6.5) * 0.5;
                    else if (isMid) baseShots = 1.0 + (p.getShooting() - 50) * 0.02 + (ratingVal - 6.5) * 0.2;
                    else if (isDef) baseShots = 0.2 + (p.getShooting() - 50) * 0.01 + (ratingVal - 6.5) * 0.05;
                    baseShots = Math.max(0.0, baseShots) * minsFactor;
                    totalShots += baseShots;

                    double baseSot = baseShots * (0.35 + (p.getShooting() - 50) * 0.004 + (ratingVal - 6.5) * 0.03);
                    totalShotsOnTarget += Math.max(0.0, Math.min(baseShots, baseSot));

                    double baseKp = 0.0;
                    if (isMid) baseKp = 1.8 + (p.getPassing() - 50) * 0.04 + (ratingVal - 6.5) * 0.4;
                    else if (isAtt) baseKp = 1.0 + (p.getPassing() - 50) * 0.02 + (ratingVal - 6.5) * 0.2;
                    else if (isDef) baseKp = 0.4 + (p.getPassing() - 50) * 0.01 + (ratingVal - 6.5) * 0.08;
                    totalKeyPasses += Math.max(0.0, baseKp) * minsFactor;

                    double baseTackles = 0.0;
                    if (isDef) baseTackles = 2.5 + (p.getDefending() - 50) * 0.04 + (ratingVal - 6.5) * 0.4;
                    else if (isMid) baseTackles = 1.2 + (p.getDefending() - 50) * 0.02 + (ratingVal - 6.5) * 0.2;
                    else if (isAtt) baseTackles = 0.3 + (p.getDefending() - 50) * 0.005 + (ratingVal - 6.5) * 0.05;
                    totalTackles += Math.max(0.0, baseTackles) * minsFactor;

                    double baseInter = 0.0;
                    if (isDef) baseInter = 2.0 + (p.getDefending() - 50) * 0.03 + (ratingVal - 6.5) * 0.3;
                    else if (isMid) baseInter = 1.0 + (p.getDefending() - 50) * 0.015 + (ratingVal - 6.5) * 0.15;
                    totalInterceptions += Math.max(0.0, baseInter) * minsFactor;

                    double baseBlocks = 0.0;
                    if (isDef) baseBlocks = 1.2 + (p.getDefending() - 50) * 0.02 + (ratingVal - 6.5) * 0.2;
                    totalBlocks += Math.max(0.0, baseBlocks) * minsFactor;
                }
                double passAcc = 72.0 + (p.getPassing() - 50) * 0.2 + (ratingVal - 6.5) * 2.0;
                totalPassAccuracy += Math.max(55.0, Math.min(96.0, passAcc));
            }

            int finalShots = (int) Math.round(totalShots);
            int finalShotsOnTarget = (int) Math.round(totalShotsOnTarget);
            if (finalShotsOnTarget < goals) finalShotsOnTarget = goals;
            if (finalShots < finalShotsOnTarget) finalShots = finalShotsOnTarget;

            double avgRating = appearances > 0 ? (Math.round((totalRating / appearances) * 100.0) / 100.0) : 0.0;
            double avgPassAccuracy = appearances > 0 ? (Math.round((totalPassAccuracy / appearances) * 100.0) / 100.0) : 0.0;
            double savePercentage = totalShotsFaced > 0 ? (Math.round(((double) totalSaves / totalShotsFaced * 100.0) * 100.0) / 100.0) : 0.0;
            if (totalShotsFaced == 0 && "GK".equalsIgnoreCase(p.getPosition() != null ? p.getPosition().name() : "") && cleanSheets > 0) {
                savePercentage = 100.0;
            }

            stats.add(PlayerStatisticsResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .country(p.getCountry() != null ? p.getCountry().getName() : "Unknown")
                    .position(p.getPosition() != null ? p.getPosition().name() : "Unknown")
                    .overallRating(p.getOverallRating())
                    .goals(goals)
                    .assists(assists)
                    .cleanSheets(cleanSheets)
                    .motmAwards((int) (long) motmAwards.getOrDefault(p.getId(), 0L))
                    .averageRating(avgRating)
                    .minutesPlayed(totalMins)
                    .yellowCards(yellowCards)
                    .redCards(redCards)
                    .shots(finalShots)
                    .shotsOnTarget(finalShotsOnTarget)
                    .penaltiesScored(penaltiesScored)
                    .penaltiesMissed(penaltiesMissed)
                    .ownGoals(ownGoals)
                    .passAccuracy(avgPassAccuracy)
                    .keyPasses((int) Math.round(totalKeyPasses))
                    .tackles((int) Math.round(totalTackles))
                    .interceptions((int) Math.round(totalInterceptions))
                    .blocks((int) Math.round(totalBlocks))
                    .saves(totalSaves)
                    .savePercentage(savePercentage)
                    .build());
        }
        return stats;
    }

    private int calculateMinutes(Match match, Player player, List<MatchEvent> matchEvents) {
        if (match == null) return 90;
        boolean wentToExtraTime = false;
        if (match.getRound() != null && match.getRound() != MatchRound.GROUP_STAGE) {
            if (matchEvents != null) {
                long eventGoals = matchEvents.stream()
                        .filter(e -> e.getEventType() == MatchEventType.GOAL || e.getEventType() == MatchEventType.PENALTY)
                        .count();
                int dbGoals = (match.getHomeScore() != null ? match.getHomeScore() : 0) + (match.getAwayScore() != null ? match.getAwayScore() : 0);
                boolean isShootout = dbGoals > eventGoals;
                wentToExtraTime = isShootout || matchEvents.stream().anyMatch(e -> e.getMinute() != null && e.getMinute() > 90);
            }
        }
        int totalMinutes = wentToExtraTime ? 120 : 90;
        if (matchEvents == null) return totalMinutes;

        for (MatchEvent event : matchEvents) {
            if (event.getEventType() == MatchEventType.SUBSTITUTION) {
                if (event.getPlayer() != null && event.getPlayer().getId().equals(player.getId())) {
                    return Math.max(0, totalMinutes - (event.getMinute() != null ? event.getMinute() : 60));
                }
                if (event.getDescription() != null && event.getDescription().contains("replaces " + player.getName())) {
                    return event.getMinute() != null ? event.getMinute() : 60;
                }
            }
        }
        return totalMinutes;
    }

    private List<TeamStatisticsResponse> computeAllTeamStats() {
        List<Team> allTeams = teamRepository.findAll();
        List<Match> allMatches = matchRepository.findAll();
        List<MatchStatistics> allMatchStats = matchStatisticsRepository.findAll();
        List<PlayerMatchRating> allRatings = playerMatchRatingRepository.findAll();
        List<Squad> allSquads = squadRepository.findAll();

        Map<Long, List<Match>> matchesByTeam = new HashMap<>();
        for (Match m : allMatches) {
            if (m.getStatus() != MatchStatus.FINISHED) continue;
            if (m.getHomeTeam() != null) matchesByTeam.computeIfAbsent(m.getHomeTeam().getId(), k -> new ArrayList<>()).add(m);
            if (m.getAwayTeam() != null) matchesByTeam.computeIfAbsent(m.getAwayTeam().getId(), k -> new ArrayList<>()).add(m);
        }

        Map<Long, MatchStatistics> matchStatsById = allMatchStats.stream().collect(Collectors.toMap(ms -> ms.getMatch().getId(), ms -> ms, (a, b) -> a));
        Map<Long, List<PlayerMatchRating>> ratingsByMatch = allRatings.stream().collect(Collectors.groupingBy(r -> r.getMatch().getId()));
        Map<Long, Squad> squadByTeam = allSquads.stream().filter(s -> s.getTeam() != null).collect(Collectors.toMap(s -> s.getTeam().getId(), s -> s, (a, b) -> a));

        List<TeamStatisticsResponse> stats = new ArrayList<>();
        for (Team t : allTeams) {
            List<Match> tMatches = matchesByTeam.getOrDefault(t.getId(), Collections.emptyList());
            int played = tMatches.size();
            int wins = 0;
            int draws = 0;
            int losses = 0;
            int goalsScored = 0;
            int goalsConceded = 0;
            int cleanSheets = 0;

            double totalPossession = 0.0;
            double totalXG = 0.0;
            double totalPassAccuracy = 0.0;

            double sumTeamPlayerRating = 0.0;
            int ratingCount = 0;

            for (Match m : tMatches) {
                boolean isHome = m.getHomeTeam() != null && t.getId().equals(m.getHomeTeam().getId());
                int scored = isHome ? m.getHomeScore() : m.getAwayScore();
                int conceded = isHome ? m.getAwayScore() : m.getHomeScore();

                goalsScored += scored;
                goalsConceded += conceded;
                if (conceded == 0) cleanSheets++;

                if (scored > conceded) wins++;
                else if (scored == conceded) draws++;
                else losses++;

                MatchStatistics ms = matchStatsById.get(m.getId());
                if (ms != null) {
                    totalPossession += isHome ? (ms.getHomePossession() != null ? ms.getHomePossession() : 50) : (ms.getAwayPossession() != null ? ms.getAwayPossession() : 50);
                    totalXG += isHome ? (ms.getHomeExpectedGoals() != null ? ms.getHomeExpectedGoals() : 0.0) : (ms.getAwayExpectedGoals() != null ? ms.getAwayExpectedGoals() : 0.0);
                    totalPassAccuracy += isHome ? (ms.getHomePassAccuracy() != null ? ms.getHomePassAccuracy() : 80) : (ms.getAwayPassAccuracy() != null ? ms.getAwayPassAccuracy() : 80);
                } else {
                    totalPossession += 50.0;
                    totalPassAccuracy += 80.0;
                }

                List<PlayerMatchRating> mRatings = ratingsByMatch.getOrDefault(m.getId(), Collections.emptyList());
                for (PlayerMatchRating r : mRatings) {
                    if (r.getPlayer() != null && r.getPlayer().getCountry() != null && t.getCountry() != null && r.getPlayer().getCountry().getId().equals(t.getCountry().getId())) {
                        sumTeamPlayerRating += r.getRating();
                        ratingCount++;
                    }
                }
            }

            double winPercentage = played > 0 ? (Math.round(((double) wins / played * 100.0) * 100.0) / 100.0) : 0.0;
            double avgPossession = played > 0 ? (Math.round((totalPossession / played) * 10.0) / 10.0) : 0.0;
            double avgXG = played > 0 ? (Math.round((totalXG / played) * 100.0) / 100.0) : 0.0;
            double avgPassAcc = played > 0 ? (Math.round((totalPassAccuracy / played) * 100.0) / 100.0) : 0.0;
            double avgPlayerRating = ratingCount > 0 ? (Math.round((sumTeamPlayerRating / ratingCount) * 100.0) / 100.0) : 0.0;

            Squad squad = squadByTeam.get(t.getId());
            Map<String, Long> formationUsage = new HashMap<>();
            if (squad != null && squad.getFormation() != null && played > 0) {
                formationUsage.put(squad.getFormation().getName(), (long) played);
            }

            stats.add(TeamStatisticsResponse.builder()
                    .teamId(t.getId())
                    .teamName(t.getName())
                    .matchesPlayed(played)
                    .wins(wins)
                    .draws(draws)
                    .losses(losses)
                    .winPercentage(winPercentage)
                    .goalsScored(goalsScored)
                    .goalsConceded(goalsConceded)
                    .cleanSheets(cleanSheets)
                    .averagePossession(avgPossession)
                    .averageExpectedGoals(avgXG)
                    .passAccuracy(avgPassAcc)
                    .averagePlayerRating(avgPlayerRating)
                    .formationUsage(formationUsage)
                    .build());
        }
        return stats;
    }

    private List<TournamentStatisticsResponse> computeAllTournamentStats() {
        List<Tournament> allTournaments = tournamentRepository.findAll();
        List<Match> allMatches = matchRepository.findAll();
        List<MatchEvent> allEvents = matchEventRepository.findAll();

        Map<Long, List<Match>> matchesByTournament = allMatches.stream()
                .filter(m -> m.getTournament() != null)
                .collect(Collectors.groupingBy(m -> m.getTournament().getId()));

        List<TournamentStatisticsResponse> stats = new ArrayList<>();
        for (Tournament tour : allTournaments) {
            var tournamentSummary = tournamentSummaryService.summarize(tour.getId());
            List<Match> tMatches = matchesByTournament.getOrDefault(tour.getId(), Collections.emptyList());
            List<Match> completed = tMatches.stream()
                    .filter(m -> m.getStatus() == MatchStatus.FINISHED && m.getHomeScore() != null && m.getAwayScore() != null)
                    .toList();

            int totalGoals = tournamentSummary.totalGoals() == null
                    ? completed.stream().mapToInt(m -> m.getHomeScore() + m.getAwayScore()).sum()
                    : tournamentSummary.totalGoals();
            double avgGoals = completed.isEmpty() ? 0.0 : (Math.round(((double) totalGoals / completed.size()) * 100.0) / 100.0);

            int cleanSheets = 0;
            for (Match m : completed) {
                if (m.getHomeScore() == 0) cleanSheets++;
                if (m.getAwayScore() == 0) cleanSheets++;
            }

            List<Long> matchIds = completed.stream().map(Match::getId).toList();
            List<MatchEvent> tEvents = allEvents.stream().filter(e -> e.getMatch() != null && matchIds.contains(e.getMatch().getId())).toList();

            int penalties = (int) tEvents.stream().filter(e -> e.getEventType() == MatchEventType.PENALTY).count();
            int yellowCards = (int) tEvents.stream().filter(e -> e.getEventType() == MatchEventType.YELLOW_CARD).count();
            int redCards = (int) tEvents.stream().filter(e -> e.getEventType() == MatchEventType.RED_CARD).count();

            Match biggestWinMatch = completed.stream()
                    .max(Comparator.comparingInt(m -> Math.abs(m.getHomeScore() - m.getAwayScore())))
                    .orElse(null);
            String biggestWinStr = biggestWinMatch == null ? "N/A" : biggestWinMatch.getHomeTeam().getName() + " " + biggestWinMatch.getHomeScore() + " - " + biggestWinMatch.getAwayScore() + " " + biggestWinMatch.getAwayTeam().getName();

            Match highestScoringMatch = completed.stream()
                    .max(Comparator.comparingInt(m -> m.getHomeScore() + m.getAwayScore()))
                    .orElse(null);
            String highestScoringStr = tournamentSummary.highestScoringMatch() != null
                    ? tournamentSummary.highestScoringMatch()
                    : highestScoringMatch == null ? "N/A" : highestScoringMatch.getHomeTeam().getName() + " " + highestScoringMatch.getHomeScore() + " - " + highestScoringMatch.getAwayScore() + " " + highestScoringMatch.getAwayTeam().getName();

            String topScorerName = tournamentSummary.topScorer() == null
                    ? "N/A" : tournamentSummary.topScorer();
            String bestGkName = tournamentSummary.bestGoalkeeper() == null
                    ? "N/A" : tournamentSummary.bestGoalkeeper();
            if (tour.getStatus() == TournamentStatus.COMPLETED) {
                try {
                    var awards = tournamentAwardsService.calculateAwards(tour.getId());
                    if (awards != null) {
                        if (awards.goldenBoot() != null) topScorerName = awards.goldenBoot().player() + " (" + awards.goldenBoot().goals() + " goals)";
                        if (awards.goldenGlove() != null) bestGkName = awards.goldenGlove().player() + " (" + awards.goldenGlove().averageRating() + " rating)";
                    }
                } catch (Exception ignored) {}
            }

            String championName = "N/A";
            Match finalMatch = completed.stream()
                    .filter(m -> m.getRound() == MatchRound.FINAL)
                    .findFirst()
                    .orElse(null);
            if (finalMatch != null) {
                if (finalMatch.getHomeScore() > finalMatch.getAwayScore()) championName = finalMatch.getHomeTeam().getName();
                else if (finalMatch.getAwayScore() > finalMatch.getHomeScore()) championName = finalMatch.getAwayTeam().getName();
            }

            stats.add(TournamentStatisticsResponse.builder()
                    .tournamentId(tour.getId())
                    .tournamentName(tour.getName())
                    .year(tour.getYear() != null ? tour.getYear() : 2026)
                    .status(tour.getStatus().name())
                    .completedMatches(tournamentSummary.completedMatches() == null
                            ? completed.size() : tournamentSummary.completedMatches())
                    .totalGoals(totalGoals)
                    .averageGoals(avgGoals)
                    .biggestWin(biggestWinStr)
                    .cleanSheets(cleanSheets)
                    .penalties(penalties)
                    .yellowCards(yellowCards)
                    .redCards(redCards)
                    .highestScoringMatch(highestScoringStr)
                    .topScorer(topScorerName)
                    .bestGoalkeeper(bestGkName)
                    .champion(championName)
                    .build());
        }
        return stats;
    }

    private List<MatchStatisticsResponse> computeAllMatchStats() {
        List<Match> allMatches = matchRepository.findAll();
        List<MatchStatistics> allMatchStats = matchStatisticsRepository.findAll();

        Map<Long, MatchStatistics> statsByMatchId = allMatchStats.stream()
                .collect(Collectors.toMap(ms -> ms.getMatch().getId(), ms -> ms, (a, b) -> a));

        List<MatchStatisticsResponse> responseList = new ArrayList<>();
        for (Match m : allMatches) {
            MatchStatistics ms = statsByMatchId.get(m.getId());
            responseList.add(MatchStatisticsResponse.builder()
                    .matchId(m.getId())
                    .tournamentId(m.getTournament() != null ? m.getTournament().getId() : null)
                    .tournamentName(m.getTournament() != null ? m.getTournament().getName() : "Friendly")
                    .round(m.getRound() != null ? m.getRound().name() : "N/A")
                    .matchDate(m.getMatchDate())
                    .homeTeamId(m.getHomeTeam() != null ? m.getHomeTeam().getId() : null)
                    .homeTeamName(m.getHomeTeam() != null ? m.getHomeTeam().getName() : "Unknown")
                    .awayTeamId(m.getAwayTeam() != null ? m.getAwayTeam().getId() : null)
                    .awayTeamName(m.getAwayTeam() != null ? m.getAwayTeam().getName() : "Unknown")
                    .homeScore(m.getHomeScore())
                    .awayScore(m.getAwayScore())
                    .status(m.getStatus() != null ? m.getStatus().name() : "SCHEDULED")
                    .homePossession(ms != null && ms.getHomePossession() != null ? ms.getHomePossession() : 50.0)
                    .awayPossession(ms != null && ms.getAwayPossession() != null ? ms.getAwayPossession() : 50.0)
                    .homeShots(ms != null && ms.getHomeShots() != null ? ms.getHomeShots() : 0)
                    .awayShots(ms != null && ms.getAwayShots() != null ? ms.getAwayShots() : 0)
                    .homeShotsOnTarget(ms != null && ms.getHomeShotsOnTarget() != null ? ms.getHomeShotsOnTarget() : 0)
                    .awayShotsOnTarget(ms != null && ms.getAwayShotsOnTarget() != null ? ms.getAwayShotsOnTarget() : 0)
                    .homePasses(ms != null && ms.getHomePasses() != null ? ms.getHomePasses() : 0)
                    .awayPasses(ms != null && ms.getAwayPasses() != null ? ms.getAwayPasses() : 0)
                    .homePassAccuracy(ms != null && ms.getHomePassAccuracy() != null ? ms.getHomePassAccuracy() : 0.0)
                    .awayPassAccuracy(ms != null && ms.getAwayPassAccuracy() != null ? ms.getAwayPassAccuracy() : 0.0)
                    .homeCorners(ms != null && ms.getHomeCorners() != null ? ms.getHomeCorners() : 0)
                    .awayCorners(ms != null && ms.getAwayCorners() != null ? ms.getAwayCorners() : 0)
                    .homeFouls(ms != null && ms.getHomeFouls() != null ? ms.getHomeFouls() : 0)
                    .awayFouls(ms != null && ms.getAwayFouls() != null ? ms.getAwayFouls() : 0)
                    .homeYellowCards(ms != null && ms.getHomeYellowCards() != null ? ms.getHomeYellowCards() : 0)
                    .awayYellowCards(ms != null && ms.getAwayYellowCards() != null ? ms.getAwayYellowCards() : 0)
                    .homeRedCards(ms != null && ms.getHomeRedCards() != null ? ms.getHomeRedCards() : 0)
                    .awayRedCards(ms != null && ms.getAwayRedCards() != null ? ms.getAwayRedCards() : 0)
                    .homeSaves(ms != null && ms.getHomeSaves() != null ? ms.getHomeSaves() : 0)
                    .awaySaves(ms != null && ms.getAwaySaves() != null ? ms.getAwaySaves() : 0)
                    .homeExpectedGoals(ms != null && ms.getHomeExpectedGoals() != null ? ms.getHomeExpectedGoals() : 0.0)
                    .awayExpectedGoals(ms != null && ms.getAwayExpectedGoals() != null ? ms.getAwayExpectedGoals() : 0.0)
                    .build());
        }
        return responseList;
    }

    private FootballRecordsResponse computeRecords() {
        List<PlayerStatisticsResponse> pStats = getCachedOrCompute("allPlayerStats", this::computeAllPlayerStats);
        List<TeamStatisticsResponse> tStats = getCachedOrCompute("allTeamStats", this::computeAllTeamStats);
        List<Match> allMatches = matchRepository.findAll();
        List<MatchEvent> allEvents = matchEventRepository.findAll();

        List<Match> completedMatches = allMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.FINISHED && m.getHomeScore() != null && m.getAwayScore() != null)
                .toList();

        // Player Records
        var topScorers = leaderboardService.getHighestScoringPlayers().stream()
                .map(entry -> new RecordEntry(entry.playerId(), entry.playerName(), entry.teamName(), entry.count()))
                .toList();
        var topAssisters = leaderboardService.getMostAssists().stream()
                .map(entry -> new RecordEntry(entry.playerId(), entry.playerName(), entry.teamName(), entry.count()))
                .toList();
        var mostAppearances = getTopPlayerRecords(pStats, p -> (long) p.getMinutesPlayed() / 90, 5); // appearances
        var mostCleanSheets = getTopPlayerRecords(pStats, p -> (long) p.getCleanSheets(), 5);
        var highestAverageRatings = leaderboardService.getHighestRatedPlayers().stream()
                .map(entry -> new RecordEntry(entry.playerId(), entry.playerName(), entry.teamName(),
                        Math.round(entry.averageRating() * 100.0)))
                .toList();
        var mostYellowCards = getTopPlayerRecords(pStats, p -> (long) p.getYellowCards(), 5);
        var mostRedCards = getTopPlayerRecords(pStats, p -> (long) p.getRedCards(), 5);

        // Team Records
        List<RecordEntry> winStreaks = new ArrayList<>();
        List<RecordEntry> unbeatenStreaks = new ArrayList<>();
        List<RecordEntry> cleanSheetStreaks = new ArrayList<>();
        List<RecordEntry> biggestWins = new ArrayList<>();
        List<RecordEntry> biggestDefeats = new ArrayList<>();

        for (TeamStatisticsResponse t : tStats) {
            Team team = teamRepository.findById(t.getTeamId()).orElse(null);
            if (team == null) continue;
            List<Match> teamMatches = completedMatches.stream()
                    .filter(m -> m.getHomeTeam().getId().equals(team.getId()) || m.getAwayTeam().getId().equals(team.getId()))
                    .sorted(Comparator.comparing(Match::getMatchDate).thenComparing(Match::getId))
                    .toList();

            int maxWinStreak = 0, currentWin = 0;
            int maxUnbeaten = 0, currentUnbeaten = 0;
            int maxCleanSheet = 0, currentClean = 0;

            for (Match m : teamMatches) {
                boolean isHome = m.getHomeTeam().getId().equals(team.getId());
                int scored = isHome ? m.getHomeScore() : m.getAwayScore();
                int conceded = isHome ? m.getAwayScore() : m.getHomeScore();

                if (scored > conceded) {
                    currentWin++;
                    currentUnbeaten++;
                } else if (scored == conceded) {
                    currentWin = 0;
                    currentUnbeaten++;
                } else {
                    currentWin = 0;
                    currentUnbeaten = 0;
                }

                if (conceded == 0) currentClean++;
                else currentClean = 0;

                maxWinStreak = Math.max(maxWinStreak, currentWin);
                maxUnbeaten = Math.max(maxUnbeaten, currentUnbeaten);
                maxCleanSheet = Math.max(maxCleanSheet, currentClean);
            }

            winStreaks.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Wins Streak", maxWinStreak));
            unbeatenStreaks.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Unbeaten Streak", maxUnbeaten));
            cleanSheetStreaks.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Clean Sheets Streak", maxCleanSheet));

            int maxVictoryDiff = 0;
            int maxDefeatDiff = 0;
            for (Match m : teamMatches) {
                boolean isHome = m.getHomeTeam().getId().equals(team.getId());
                int scored = isHome ? m.getHomeScore() : m.getAwayScore();
                int conceded = isHome ? m.getAwayScore() : m.getHomeScore();
                if (scored > conceded) maxVictoryDiff = Math.max(maxVictoryDiff, scored - conceded);
                if (conceded > scored) maxDefeatDiff = Math.max(maxDefeatDiff, conceded - scored);
            }
            biggestWins.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Goal Diff", maxVictoryDiff));
            biggestDefeats.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Goal Diff", maxDefeatDiff));
        }

        // Most Titles and Finals
        List<RecordEntry> mostTitlesList = new ArrayList<>();
        List<RecordEntry> mostFinalsList = new ArrayList<>();
        List<Tournament> tournaments = tournamentRepository.findAll();
        Map<Long, Long> titleCounts = new HashMap<>();
        Map<Long, Long> finalCounts = new HashMap<>();

        for (Tournament tour : tournaments) {
            List<Match> tourMatches = completedMatches.stream().filter(m -> m.getTournament() != null && tour.getId().equals(m.getTournament().getId())).toList();
            Match finalMatch = tourMatches.stream().filter(m -> m.getRound() == MatchRound.FINAL).findFirst().orElse(null);
            if (finalMatch != null) {
                finalCounts.put(finalMatch.getHomeTeam().getId(), finalCounts.getOrDefault(finalMatch.getHomeTeam().getId(), 0L) + 1);
                finalCounts.put(finalMatch.getAwayTeam().getId(), finalCounts.getOrDefault(finalMatch.getAwayTeam().getId(), 0L) + 1);
                if (finalMatch.getHomeScore() > finalMatch.getAwayScore()) {
                    titleCounts.put(finalMatch.getHomeTeam().getId(), titleCounts.getOrDefault(finalMatch.getHomeTeam().getId(), 0L) + 1);
                } else if (finalMatch.getAwayScore() > finalMatch.getHomeScore()) {
                    titleCounts.put(finalMatch.getAwayTeam().getId(), titleCounts.getOrDefault(finalMatch.getAwayTeam().getId(), 0L) + 1);
                }
            }
        }

        for (TeamStatisticsResponse t : tStats) {
            mostTitlesList.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Titles", titleCounts.getOrDefault(t.getTeamId(), 0L)));
            mostFinalsList.add(new RecordEntry(t.getTeamId(), t.getTeamName(), "Finals Reached", finalCounts.getOrDefault(t.getTeamId(), 0L)));
        }

        // Match Records
        List<RecordEntry> highestScoringMatches = completedMatches.stream()
                .map(m -> new RecordEntry(m.getId(), m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName(), (m.getTournament() != null ? m.getTournament().getName() : "Friendly"), m.getHomeScore() + m.getAwayScore()))
                .sorted(Comparator.comparing(RecordEntry::getValue).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<RecordEntry> bigComebacksList = new ArrayList<>();
        for (Match m : completedMatches) {
            List<MatchEvent> mEvents = allEvents.stream()
                    .filter(e -> e.getMatch() != null && m.getId().equals(e.getMatch().getId()))
                    .sorted(Comparator.comparing(MatchEvent::getMinute))
                    .toList();
            
            // Calculate max deficit won
            int homeScore = 0, awayScore = 0;
            int maxDeficit = 0;
            int winnerSign = Integer.compare(m.getHomeScore(), m.getAwayScore()); // 1 for home win, -1 for away win

            if (winnerSign != 0) {
                for (MatchEvent e : mEvents) {
                    boolean isGoal = e.getEventType() == MatchEventType.GOAL 
                        || (e.getEventType() == MatchEventType.PENALTY && e.getDescription() != null && e.getDescription().toLowerCase().contains("converts"));
                    boolean isOwnGoal = e.getEventType() == MatchEventType.OWN_GOAL;
                    
                    if (isGoal || isOwnGoal) {
                        // find scorer team
                        boolean scorerIsHome = false;
                        if (e.getPlayer() != null && e.getPlayer().getCountry() != null) {
                            if (m.getHomeTeam().getCountry() != null && e.getPlayer().getCountry().getId().equals(m.getHomeTeam().getCountry().getId())) {
                                scorerIsHome = true;
                            }
                        }
                        if (isGoal) {
                            if (scorerIsHome) homeScore++;
                            else awayScore++;
                        } else {
                            // own goal
                            if (scorerIsHome) awayScore++;
                            else homeScore++;
                        }
                        
                        if (winnerSign > 0) {
                            maxDeficit = Math.max(maxDeficit, awayScore - homeScore);
                        } else {
                            maxDeficit = Math.max(maxDeficit, homeScore - awayScore);
                        }
                    }
                }
                if (maxDeficit > 0) {
                    bigComebacksList.add(new RecordEntry(m.getId(), m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName(), (winnerSign > 0 ? m.getHomeTeam().getName() : m.getAwayTeam().getName()) + " trailed by " + maxDeficit, maxDeficit));
                }
            }
        }
        bigComebacksList.sort(Comparator.comparing(RecordEntry::getValue).reversed());
        var biggestComebacks = bigComebacksList.stream().limit(5).collect(Collectors.toList());

        List<RecordEntry> longestPenaltyShootouts = completedMatches.stream()
                .map(m -> new RecordEntry(m.getId(), m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName(),
                        "Recorded penalty shootout kicks", countPenaltyShootoutKicks(m, allEvents)))
                .filter(record -> record.getValue() > 0)
                .sorted(Comparator.comparing(RecordEntry::getValue).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<RecordEntry> matchesWithMostCards = completedMatches.stream()
                .map(m -> {
                    long cardCount = allEvents.stream()
                            .filter(e -> e.getMatch() != null && m.getId().equals(e.getMatch().getId()))
                            .filter(e -> e.getEventType() == MatchEventType.YELLOW_CARD || e.getEventType() == MatchEventType.RED_CARD)
                            .count();
                    return new RecordEntry(m.getId(), m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName(), cardCount + " cards", cardCount);
                })
                .sorted(Comparator.comparing(RecordEntry::getValue).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<RecordValueEntry> fastestGoals = allEvents.stream()
                .filter(e -> e.getEventType() == MatchEventType.GOAL && e.getMatch() != null && e.getPlayer() != null)
                .map(e -> new RecordValueEntry(e.getMatch().getId(), e.getPlayer().getName(), "Match Id: " + e.getMatch().getId() + " at " + e.getMinute() + " min", (double) (e.getMinute() != null ? e.getMinute() : 90)))
                .sorted(Comparator.comparing(RecordValueEntry::getValue))
                .limit(5)
                .collect(Collectors.toList());

        List<RecordValueEntry> latestGoals = allEvents.stream()
                .filter(e -> e.getEventType() == MatchEventType.GOAL && e.getMatch() != null && e.getPlayer() != null)
                .map(e -> new RecordValueEntry(e.getMatch().getId(), e.getPlayer().getName(), "Match Id: " + e.getMatch().getId() + " at " + e.getMinute() + " min", (double) (e.getMinute() != null ? e.getMinute() : 90)))
                .sorted(Comparator.comparing(RecordValueEntry::getValue).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<RecordEntry> matchesWithSubs = completedMatches.stream()
                .map(m -> {
                    long subCount = allEvents.stream()
                            .filter(e -> e.getMatch() != null && m.getId().equals(e.getMatch().getId()))
                            .filter(e -> e.getEventType() == MatchEventType.SUBSTITUTION)
                            .count();
                    return new RecordEntry(m.getId(), m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName(), subCount + " substitutions", subCount);
                })
                .sorted(Comparator.comparing(RecordEntry::getValue).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return FootballRecordsResponse.builder()
                .playerRecords(PlayerRecords.builder()
                        .topScorers(topScorers)
                        .topAssisters(topAssisters)
                        .mostAppearances(mostAppearances)
                        .mostCleanSheets(mostCleanSheets)
                        .highestAverageRatings(highestAverageRatings)
                        .mostYellowCards(mostYellowCards)
                        .mostRedCards(mostRedCards)
                        .youngestScorers(scorersByAge(allEvents, true))
                        .oldestScorers(scorersByAge(allEvents, false))
                        .build())
                .teamRecords(TeamRecords.builder()
                        .longestWinningStreaks(getTopTeamRecords(winStreaks, 5))
                        .longestUnbeatenStreaks(getTopTeamRecords(unbeatenStreaks, 5))
                        .longestCleanSheetStreaks(getTopTeamRecords(cleanSheetStreaks, 5))
                        .biggestVictories(getTopTeamRecords(biggestWins, 5))
                        .biggestDefeats(getTopTeamRecords(biggestDefeats, 5))
                        .mostTitles(getTopTeamRecords(mostTitlesList, 5))
                        .mostFinals(getTopTeamRecords(mostFinalsList, 5))
                        .build())
                .matchRecords(MatchRecords.builder()
                        .highestScoringMatches(highestScoringMatches)
                        .biggestComebacks(biggestComebacks)
                        .longestPenaltyShootouts(longestPenaltyShootouts)
                        .mostCards(matchesWithMostCards)
                        .fastestGoals(fastestGoals)
                        .latestGoals(latestGoals)
                        .mostSubstitutions(matchesWithSubs)
                        .build())
                .build();
    }

    private boolean isShootoutMatch(Match m, List<MatchEvent> events) {
        if (m.getRound() == null || m.getRound() == MatchRound.GROUP_STAGE) return false;
        long eventGoals = events.stream()
                .filter(e -> e.getMatch() != null && m.getId().equals(e.getMatch().getId()))
                .filter(e -> e.getEventType() == MatchEventType.GOAL || e.getEventType() == MatchEventType.PENALTY)
                .count();
        int dbGoals = (m.getHomeScore() != null ? m.getHomeScore() : 0) + (m.getAwayScore() != null ? m.getAwayScore() : 0);
        return dbGoals > eventGoals;
    }

    private long countPenaltyShootoutKicks(Match match, List<MatchEvent> events) {
        return events.stream()
                .filter(event -> event.getMatch() != null && match.getId().equals(event.getMatch().getId()))
                .filter(event -> event.getEventType() == MatchEventType.PENALTY)
                .filter(event -> event.getDescription() != null
                        && event.getDescription().toLowerCase(Locale.ROOT).contains("shootout"))
                .count();
    }

    private List<RecordEntry> scorersByAge(List<MatchEvent> events, boolean youngest) {
        List<Player> scorers = events.stream()
                .filter(this::isScoringEvent)
                .map(MatchEvent::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> player.getAge() != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Player::getId, player -> player, (first, ignored) -> first),
                        players -> new ArrayList<>(players.values())));
        if (scorers.isEmpty()) {
            return Collections.emptyList();
        }
        Comparator<Player> comparator = Comparator.comparing(Player::getAge);
        int age = (youngest ? scorers.stream().min(comparator) : scorers.stream().max(comparator))
                .map(Player::getAge)
                .orElse(0);
        return scorers.stream()
                .filter(player -> player.getAge() == age)
                .sorted(Comparator.comparing(Player::getName))
                .map(player -> new RecordEntry(player.getId(), player.getName(),
                        player.getCountry() == null ? "Unknown" : player.getCountry().getName(), age))
                .toList();
    }

    private boolean isScoringEvent(MatchEvent event) {
        return event.getEventType() == MatchEventType.GOAL
                || (event.getEventType() == MatchEventType.PENALTY
                && event.getDescription() != null
                && event.getDescription().toLowerCase(Locale.ROOT).contains("converts"));
    }

    private List<RecordEntry> getTopPlayerRecords(List<PlayerStatisticsResponse> players,
                                                 java.util.function.Function<PlayerStatisticsResponse, Long> extractor,
                                                 int limit) {
        if (players.isEmpty()) return Collections.emptyList();
        List<PlayerStatisticsResponse> sorted = players.stream()
                .sorted(Comparator.comparing(extractor).reversed())
                .toList();
        long cutoff = sorted.size() > limit ? extractor.apply(sorted.get(limit - 1)) : 0L;
        return sorted.stream()
                .filter(p -> extractor.apply(p) >= cutoff && extractor.apply(p) > 0)
                .map(p -> new RecordEntry(p.getId(), p.getName(), p.getCountry(), extractor.apply(p)))
                .toList();
    }

    private List<RecordEntry> getTopPlayerDoubleRecords(List<PlayerStatisticsResponse> players,
                                                       java.util.function.Function<PlayerStatisticsResponse, Double> extractor,
                                                       int limit) {
        if (players.isEmpty()) return Collections.emptyList();
        List<PlayerStatisticsResponse> sorted = players.stream()
                .filter(p -> p.getMinutesPlayed() >= 270) // minimum 3 matches equivalent
                .sorted(Comparator.comparing(extractor).reversed())
                .toList();
        double cutoff = sorted.size() > limit ? extractor.apply(sorted.get(limit - 1)) : 0.0;
        return sorted.stream()
                .filter(p -> extractor.apply(p) >= cutoff && extractor.apply(p) > 0)
                .map(p -> new RecordEntry(p.getId(), p.getName(), p.getCountry(), Math.round(extractor.apply(p) * 100) / 100))
                .toList();
    }

    private List<RecordEntry> getTopTeamRecords(List<RecordEntry> teamRecords, int limit) {
        if (teamRecords.isEmpty()) return Collections.emptyList();
        List<RecordEntry> sorted = teamRecords.stream()
                .sorted(Comparator.comparing(RecordEntry::getValue).reversed())
                .toList();
        long cutoff = sorted.size() > limit ? sorted.get(limit - 1).getValue() : 0L;
        return sorted.stream()
                .filter(t -> t.getValue() >= cutoff && t.getValue() > 0)
                .toList();
    }

    private StatisticsSummaryResponse computeSummary() {
        long tourCount = tournamentRepository.count();
        long teamCount = teamRepository.count();
        long playerCount = playerRepository.count();
        long managerCount = managerRepository.count();

        List<Match> matches = matchRepository.findAll();
        long simCompleted = matches.stream().filter(m -> m.getStatus() == MatchStatus.FINISHED).count();
        long totalGoals = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.FINISHED && m.getHomeScore() != null && m.getAwayScore() != null)
                .mapToLong(m -> m.getHomeScore() + m.getAwayScore())
                .sum();
        double avgGoals = simCompleted == 0 ? 0.0 : (Math.round(((double) totalGoals / simCompleted) * 100.0) / 100.0);

        return StatisticsSummaryResponse.builder()
                .totalTournaments(tourCount)
                .totalTeams(teamCount)
                .totalPlayers(playerCount)
                .totalMatchesSimulated(simCompleted)
                .totalGoalsScored(totalGoals)
                .averageGoalsPerMatch(avgGoals)
                .activeManagers(managerCount)
                .build();
    }

    private Comparator<PlayerStatisticsResponse> getPlayerComparator(String property) {
        return switch (property) {
            case "name" -> Comparator.comparing(PlayerStatisticsResponse::getName);
            case "country" -> Comparator.comparing(PlayerStatisticsResponse::getCountry);
            case "position" -> Comparator.comparing(PlayerStatisticsResponse::getPosition);
            case "overallRating" -> Comparator.comparingInt(PlayerStatisticsResponse::getOverallRating);
            case "goals" -> Comparator.comparingInt(PlayerStatisticsResponse::getGoals);
            case "assists" -> Comparator.comparingInt(PlayerStatisticsResponse::getAssists);
            case "cleanSheets" -> Comparator.comparingInt(PlayerStatisticsResponse::getCleanSheets);
            case "motmAwards" -> Comparator.comparingInt(PlayerStatisticsResponse::getMotmAwards);
            case "averageRating" -> Comparator.comparingDouble(PlayerStatisticsResponse::getAverageRating);
            case "minutesPlayed" -> Comparator.comparingInt(PlayerStatisticsResponse::getMinutesPlayed);
            case "yellowCards" -> Comparator.comparingInt(PlayerStatisticsResponse::getYellowCards);
            case "redCards" -> Comparator.comparingInt(PlayerStatisticsResponse::getRedCards);
            case "shots" -> Comparator.comparingInt(PlayerStatisticsResponse::getShots);
            case "shotsOnTarget" -> Comparator.comparingInt(PlayerStatisticsResponse::getShotsOnTarget);
            case "penaltiesScored" -> Comparator.comparingInt(PlayerStatisticsResponse::getPenaltiesScored);
            case "penaltiesMissed" -> Comparator.comparingInt(PlayerStatisticsResponse::getPenaltiesMissed);
            case "ownGoals" -> Comparator.comparingInt(PlayerStatisticsResponse::getOwnGoals);
            case "passAccuracy" -> Comparator.comparingDouble(PlayerStatisticsResponse::getPassAccuracy);
            case "keyPasses" -> Comparator.comparingInt(PlayerStatisticsResponse::getKeyPasses);
            case "tackles" -> Comparator.comparingInt(PlayerStatisticsResponse::getTackles);
            case "interceptions" -> Comparator.comparingInt(PlayerStatisticsResponse::getInterceptions);
            case "blocks" -> Comparator.comparingInt(PlayerStatisticsResponse::getBlocks);
            case "saves" -> Comparator.comparingInt(PlayerStatisticsResponse::getSaves);
            case "savePercentage" -> Comparator.comparingDouble(PlayerStatisticsResponse::getSavePercentage);
            default -> Comparator.comparing(PlayerStatisticsResponse::getId);
        };
    }

    private Comparator<TeamStatisticsResponse> getTeamComparator(String property) {
        return switch (property) {
            case "name", "teamName" -> Comparator.comparing(TeamStatisticsResponse::getTeamName);
            case "matchesPlayed" -> Comparator.comparingInt(TeamStatisticsResponse::getMatchesPlayed);
            case "wins" -> Comparator.comparingInt(TeamStatisticsResponse::getWins);
            case "losses" -> Comparator.comparingInt(TeamStatisticsResponse::getLosses);
            case "winPercentage" -> Comparator.comparingDouble(TeamStatisticsResponse::getWinPercentage);
            case "goalsScored" -> Comparator.comparingInt(TeamStatisticsResponse::getGoalsScored);
            case "goalsConceded" -> Comparator.comparingInt(TeamStatisticsResponse::getGoalsConceded);
            case "cleanSheets" -> Comparator.comparingInt(TeamStatisticsResponse::getCleanSheets);
            case "averagePossession" -> Comparator.comparingDouble(TeamStatisticsResponse::getAveragePossession);
            default -> Comparator.comparing(TeamStatisticsResponse::getTeamId);
        };
    }

    private Comparator<TournamentStatisticsResponse> getTournamentComparator(String property) {
        return switch (property) {
            case "name", "tournamentName" -> Comparator.comparing(TournamentStatisticsResponse::getTournamentName);
            case "year" -> Comparator.comparingInt(TournamentStatisticsResponse::getYear);
            case "completedMatches" -> Comparator.comparingInt(TournamentStatisticsResponse::getCompletedMatches);
            case "totalGoals" -> Comparator.comparingInt(TournamentStatisticsResponse::getTotalGoals);
            case "averageGoals" -> Comparator.comparingDouble(TournamentStatisticsResponse::getAverageGoals);
            default -> Comparator.comparing(TournamentStatisticsResponse::getTournamentId);
        };
    }

    private Comparator<MatchStatisticsResponse> getMatchComparator(String property) {
        return switch (property) {
            case "matchDate" -> Comparator.comparing(MatchStatisticsResponse::getMatchDate);
            case "round" -> Comparator.comparing(MatchStatisticsResponse::getRound);
            case "homeScore" -> Comparator.comparingInt(MatchStatisticsResponse::getHomeScore);
            case "awayScore" -> Comparator.comparingInt(MatchStatisticsResponse::getAwayScore);
            default -> Comparator.comparing(MatchStatisticsResponse::getMatchId);
        };
    }
}
