package com.aditya.worldcup.historical.service;

import com.aditya.worldcup.historical.dto.*;
import com.aditya.worldcup.managers.entity.*;
import com.aditya.worldcup.managers.repository.*;
import com.aditya.worldcup.matches.entity.*;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.optimization.config.OptimizationProperties;
import com.aditya.worldcup.optimization.service.AnalyticsService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.statistics.dto.*;
import com.aditya.worldcup.statistics.service.StatisticsService;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.tournaments.dto.TournamentAwardsResponse;
import com.aditya.worldcup.tournaments.dto.TournamentSummaryResponse;
import com.aditya.worldcup.tournaments.entity.*;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournaments.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoricalIntelligenceService {

    private final StatisticsService statisticsService;
    private final AnalyticsService analyticsService;
    private final TournamentSummaryService tournamentSummaryService;
    private final TournamentAwardsService tournamentAwardsService;
    private final TournamentIntelligenceService tournamentIntelligenceService;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final PlayerMatchRatingRepository ratingRepository;
    private final ManagerRepository managerRepository;
    private final CareerStatisticsRepository careerStatisticsRepository;
    private final CareerHistoryRepository careerHistoryRepository;
    private final ManagerAchievementRepository achievementRepository;
    private final ManagerCareerAnalyticsRepository managerAnalyticsRepository;
    private final OptimizationProperties properties;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Page<PlayerLegacyResponse> players(String name, Pageable pageable) {
        return page(filter(playerLegacies(), name, PlayerLegacyResponse::playerName), pageable,
                Comparator.comparingLong(PlayerLegacyResponse::legacyScore).reversed());
    }

    @Transactional(readOnly = true)
    public Page<TeamLegacyResponse> teams(String name, Pageable pageable) {
        return page(filter(teamLegacies(), name, TeamLegacyResponse::teamName), pageable,
                Comparator.comparingLong(TeamLegacyResponse::legacyScore).reversed());
    }

    @Transactional(readOnly = true)
    public Page<ManagerLegacyResponse> managers(String name, Pageable pageable) {
        return page(filter(managerLegacies(), name, ManagerLegacyResponse::managerName), pageable,
                Comparator.comparingLong(ManagerLegacyResponse::legacyScore).reversed());
    }

    @Transactional(readOnly = true)
    public HallOfFameResponse hallOfFame() {
        return cached("hall", () -> new HallOfFameResponse(
                playerLegacies().stream().sorted(Comparator.comparingLong(PlayerLegacyResponse::legacyScore).reversed()).limit(20)
                        .map(p -> new HallOfFameResponse.Entry(p.playerId(), p.playerName(), p.legacyScore(), p.nation())).toList(),
                teamLegacies().stream().sorted(Comparator.comparingLong(TeamLegacyResponse::legacyScore).reversed()).limit(20)
                        .map(t -> new HallOfFameResponse.Entry(t.teamId(), t.teamName(), t.legacyScore(), t.worldCupTitles() + " titles")).toList(),
                managerLegacies().stream().sorted(Comparator.comparingLong(ManagerLegacyResponse::legacyScore).reversed()).limit(20)
                        .map(m -> new HallOfFameResponse.Entry(m.managerId(), m.managerName(), m.legacyScore(), m.reputation())).toList()));
    }

    @Transactional(readOnly = true)
    public List<RivalryResponse> rivalries(String type) {
        String normalized = type == null ? "TEAM" : type.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PLAYER" -> cached("playerRivalries", this::playerRivalries);
            case "MANAGER" -> cached("managerRivalries", this::managerRivalries);
            default -> cached("teamRivalries", this::teamRivalries);
        };
    }

    @Transactional(readOnly = true)
    public HeadToHeadResponse headToHead(String type, Long firstId, Long secondId) {
        if (firstId == null || secondId == null) throw new IllegalArgumentException("Both historical ids are required");
        return "MANAGER".equalsIgnoreCase(type) ? managerHeadToHead(firstId, secondId) : teamHeadToHead(firstId, secondId);
    }

    @Transactional(readOnly = true)
    public EraAnalysisResponse eras() {
        return cached("eras", this::computeEras);
    }

    @Transactional(readOnly = true)
    public HistoricalTimelineResponse timeline() {
        return cached("timeline", this::computeTimeline);
    }

    @Transactional(readOnly = true)
    public GlobalRankingResponse rankings() {
        return cached("rankings", () -> new GlobalRankingResponse(
                ranking(playerLegacies(), PlayerLegacyResponse::playerId, PlayerLegacyResponse::playerName, PlayerLegacyResponse::legacyScore,
                        p -> p.trophiesWon() + " trophies"),
                ranking(teamLegacies(), TeamLegacyResponse::teamId, TeamLegacyResponse::teamName, TeamLegacyResponse::legacyScore,
                        t -> t.worldCupTitles() + " titles"),
                ranking(managerLegacies(), ManagerLegacyResponse::managerId, ManagerLegacyResponse::managerName, ManagerLegacyResponse::legacyScore,
                        m -> m.trophies() + " trophies")));
    }

    @Transactional(readOnly = true)
    public HistoricalSummaryResponse summary() {
        return cached("summary", () -> {
            List<PlayerLegacyResponse> players = playerLegacies();
            List<TeamLegacyResponse> teams = teamLegacies();
            List<ManagerLegacyResponse> managers = managerLegacies();
            return new HistoricalSummaryResponse(players.size(), teams.size(), managers.size(),
                    tournamentRepository.countByStatus(TournamentStatus.COMPLETED),
                    matchRepository.findCompletedMatchesHistory().size(), topName(players, PlayerLegacyResponse::legacyScore, PlayerLegacyResponse::playerName),
                    topName(teams, TeamLegacyResponse::legacyScore, TeamLegacyResponse::teamName),
                    topName(managers, ManagerLegacyResponse::legacyScore, ManagerLegacyResponse::managerName));
        });
    }

    public void clearCache() { cache.clear(); }

    private List<PlayerLegacyResponse> playerLegacies() {
        return cached("players", this::computePlayerLegacies);
    }

    private List<TeamLegacyResponse> teamLegacies() {
        return cached("teams", this::computeTeamLegacies);
    }

    private List<ManagerLegacyResponse> managerLegacies() {
        return cached("managers", this::computeManagerLegacies);
    }

    private List<PlayerLegacyResponse> computePlayerLegacies() {
        Map<Long, PlayerStatisticsResponse> stats = statisticsService.searchPlayers(null, null, null, Pageable.unpaged())
                .getContent().stream().collect(Collectors.toMap(PlayerStatisticsResponse::getId, Function.identity()));
        List<Match> matches = matchRepository.findCompletedMatchesHistory();
        Map<Long, List<PlayerMatchRating>> ratings = ratingRepository.findAll().stream().filter(r -> r.getPlayer() != null && r.getMatch() != null)
                .collect(Collectors.groupingBy(r -> r.getPlayer().getId()));
        AwardCounts awards = awardCounts();
        Set<Long> recordHolders = recordHolders();
        return playerRepository.findAll().stream().map(player -> {
            PlayerStatisticsResponse stat = stats.get(player.getId());
            List<PlayerMatchRating> playerRatings = ratings.getOrDefault(player.getId(), List.of());
            int appearances = (int) playerRatings.stream().map(r -> r.getMatch().getTournament() == null ? null : r.getMatch().getTournament().getId()).filter(Objects::nonNull).distinct().count();
            List<Match> finals = playerRatings.stream().map(PlayerMatchRating::getMatch).filter(m -> m.getRound() == MatchRound.FINAL).toList();
            int finalsWon = (int) finals.stream().filter(m -> wonByPlayerNation(player, m)).count();
            int trophies = finalsWon;
            int captainAppearances = 0;
            int records = recordHolders.contains(player.getId()) ? 1 : 0;
            int goldenBoots = awards.boots.getOrDefault(player.getId(), 0);
            int goldenBalls = awards.balls.getOrDefault(player.getId(), 0);
            int goldenGloves = awards.gloves.getOrDefault(player.getId(), 0);
            long score = safe(stat == null ? 0 : stat.getGoals()) + safe(stat == null ? 0 : stat.getAssists())
                    + safe(stat == null ? 0 : stat.getMotmAwards()) + trophies * 10L
                    + (goldenBoots + goldenBalls + goldenGloves) * 8L + finals.size() + records * 5L;
            return new PlayerLegacyResponse(player.getId(), player.getName(), nation(player), score, trophies,
                    goldenBoots, goldenBalls, goldenGloves, safe(stat == null ? 0 : stat.getMotmAwards()), appearances,
                    finals.size(), finalsWon, captainAppearances, records);
        }).toList();
    }

    private List<TeamLegacyResponse> computeTeamLegacies() {
        Map<Long, TeamStatisticsResponse> stats = statisticsService.searchTeams(null, Pageable.unpaged()).getContent().stream()
                .collect(Collectors.toMap(TeamStatisticsResponse::getTeamId, Function.identity()));
        List<Match> matches = matchRepository.findCompletedMatchesHistory();
        return teamRepository.findAll().stream().map(team -> {
            TeamStatisticsResponse stat = stats.get(team.getId());
            List<Match> teamMatches = matches.stream().filter(m -> plays(team, m)).sorted(Comparator.comparing(Match::getMatchDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Match::getId)).toList();
            int titles = (int) teamMatches.stream().filter(m -> m.getRound() == MatchRound.FINAL && won(team, m)).count();
            int finals = (int) teamMatches.stream().filter(m -> m.getRound() == MatchRound.FINAL).count();
            int semis = (int) teamMatches.stream().filter(m -> m.getRound() == MatchRound.SEMI_FINALS).count();
            int biggestVictory = teamMatches.stream().mapToInt(m -> score(team, m) - opponentScore(team, m)).max().orElse(0);
            int winning = streak(team, teamMatches, Streak.WINNING);
            int unbeaten = streak(team, teamMatches, Streak.UNBEATEN);
            long score = titles * 25L + safe(stat == null ? 0 : stat.getWins()) * 3L
                    + safe(stat == null ? 0 : stat.getGoalsScored()) + safe(stat == null ? 0 : stat.getCleanSheets()) * 2L
                    + finals * 5L + semis * 2L;
            return new TeamLegacyResponse(team.getId(), team.getName(), score, titles, finals, semis,
                    safe(stat == null ? 0 : stat.getWins()), stat == null ? 0 : stat.getWinPercentage(),
                    safe(stat == null ? 0 : stat.getGoalsScored()), safe(stat == null ? 0 : stat.getCleanSheets()),
                    Math.max(0, biggestVictory), unbeaten, winning);
        }).toList();
    }

    private List<ManagerLegacyResponse> computeManagerLegacies() {
        Map<Long, CareerStatistics> careers = careerStatisticsRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getManager().getId(), Function.identity()));
        Map<Long, ManagerCareerAnalytics> analytics = managerAnalyticsRepository.findAll().stream()
                .collect(Collectors.toMap(a -> a.getManager().getId(), Function.identity()));
        Map<Long, Integer> achievements = achievementRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> a.getManager().getId(), Collectors.summingInt(a -> 1)));
        Map<Long, Integer> finals = careerHistoryRepository.findAll().stream()
                .collect(Collectors.groupingBy(h -> h.getManager().getId(), Collectors.summingInt(h -> h.getFinishingPosition() <= 2 ? 1 : 0)));
        return managerRepository.findAll().stream().map(manager -> {
            CareerStatistics career = careers.get(manager.getId());
            ManagerCareerAnalytics analytic = analytics.get(manager.getId());
            int trophies = safe(career == null ? 0 : career.getTrophiesWon());
            int achievementCount = achievements.getOrDefault(manager.getId(), 0);
            int promotionCount = Math.max(0, safe(manager.getLevel()) - 1);
            double win = analytic == null ? (career == null || safe(career.getMatchesManaged()) == 0 ? 0
                    : round(career.getWins() * 100.0 / career.getMatchesManaged())) : analytic.getWinPercentage();
            long score = trophies * 25L + achievementCount * 5L + safe(manager.getLevel()) * 2L
                    + Math.round(win) + finals.getOrDefault(manager.getId(), 0) * 5L;
            return new ManagerLegacyResponse(manager.getId(), manager.getDisplayName(), manager.getReputation().name(),
                    safe(manager.getLevel()), trophies, achievementCount, promotionCount, win,
                    finals.getOrDefault(manager.getId(), 0), score);
        }).toList();
    }

    private List<RivalryResponse> teamRivalries() {
        Map<String, RivalryAccumulator> rivalries = new HashMap<>();
        for (Match match : matchRepository.findCompletedMatchesHistory()) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null) continue;
            Long low = Math.min(match.getHomeTeam().getId(), match.getAwayTeam().getId());
            Long high = Math.max(match.getHomeTeam().getId(), match.getAwayTeam().getId());
            RivalryAccumulator accumulator = rivalries.computeIfAbsent(low + ":" + high,
                    ignored -> new RivalryAccumulator("TEAM", low, high,
                            low.equals(match.getHomeTeam().getId()) ? match.getHomeTeam().getName() : match.getAwayTeam().getName(),
                            high.equals(match.getHomeTeam().getId()) ? match.getHomeTeam().getName() : match.getAwayTeam().getName()));
            accumulator.add(match, low.equals(match.getHomeTeam().getId()));
        }
        return rivalries.values().stream().map(RivalryAccumulator::response)
                .filter(r -> r.meetings() > 0).sorted(Comparator.comparingInt(RivalryResponse::meetings).reversed()).toList();
    }

    private List<RivalryResponse> playerRivalries() {
        Map<Long, List<PlayerMatchRating>> ratingsByMatch = ratingRepository.findAll().stream()
                .filter(r -> r.getPlayer() != null && r.getMatch() != null)
                .collect(Collectors.groupingBy(r -> r.getMatch().getId()));
        Map<String, RivalryAccumulator> rivalries = new HashMap<>();
        for (List<PlayerMatchRating> ratings : ratingsByMatch.values()) {
            if (ratings.isEmpty()) continue;
            Match match = ratings.get(0).getMatch();
            if (match.getStatus() != MatchStatus.FINISHED) continue;
            for (int i = 0; i < ratings.size(); i++) for (int j = i + 1; j < ratings.size(); j++) {
                Player first = ratings.get(i).getPlayer(); Player second = ratings.get(j).getPlayer();
                if (first.getCountry() == null || second.getCountry() == null || first.getCountry().getId().equals(second.getCountry().getId())) continue;
                Long low = Math.min(first.getId(), second.getId()); Long high = Math.max(first.getId(), second.getId());
                RivalryAccumulator accumulator = rivalries.computeIfAbsent(low + ":" + high,
                        ignored -> new RivalryAccumulator("PLAYER", low, high,
                                low.equals(first.getId()) ? first.getName() : second.getName(),
                                high.equals(first.getId()) ? first.getName() : second.getName()));
                boolean lowIsHome = playerIsHome(low.equals(first.getId()) ? first : second, match);
                accumulator.add(match, lowIsHome);
            }
        }
        return rivalries.values().stream().map(RivalryAccumulator::response)
                .sorted(Comparator.comparingInt(RivalryResponse::meetings).reversed()).limit(100).toList();
    }

    private List<RivalryResponse> managerRivalries() {
        Map<Long, List<CareerHistory>> historyByTournament = careerHistoryRepository.findAll().stream()
                .collect(Collectors.groupingBy(history -> history.getTournament().getId()));
        Map<String, RivalryAccumulator> rivalries = new HashMap<>();
        for (List<CareerHistory> histories : historyByTournament.values()) {
            for (int i = 0; i < histories.size(); i++) for (int j = i + 1; j < histories.size(); j++) {
                CareerHistory first = histories.get(i), second = histories.get(j);
                if (first.getManager().getId().equals(second.getManager().getId())) continue;
                for (Match match : matchRepository.findByTournamentIdOrderById(first.getTournament().getId())) {
                    if (match.getStatus() != MatchStatus.FINISHED || !plays(first.getTeam(), match) || !plays(second.getTeam(), match)) continue;
                    Long low = Math.min(first.getManager().getId(), second.getManager().getId()); Long high = Math.max(first.getManager().getId(), second.getManager().getId());
                    RivalryAccumulator accumulator = rivalries.computeIfAbsent(low + ":" + high,
                            ignored -> new RivalryAccumulator("MANAGER", low, high,
                                    low.equals(first.getManager().getId()) ? first.getManager().getDisplayName() : second.getManager().getDisplayName(),
                                    high.equals(first.getManager().getId()) ? first.getManager().getDisplayName() : second.getManager().getDisplayName()));
                    accumulator.add(match, low.equals(first.getManager().getId()) ? plays(first.getTeam(), match) && match.getHomeTeam().getId().equals(first.getTeam().getId())
                            : plays(second.getTeam(), match) && match.getHomeTeam().getId().equals(second.getTeam().getId()));
                }
            }
        }
        return rivalries.values().stream().map(RivalryAccumulator::response)
                .sorted(Comparator.comparingInt(RivalryResponse::meetings).reversed()).toList();
    }

    private HeadToHeadResponse teamHeadToHead(Long firstId, Long secondId) {
        Team first = teamRepository.findById(firstId).orElseThrow(() -> new IllegalArgumentException("Team not found: " + firstId));
        Team second = teamRepository.findById(secondId).orElseThrow(() -> new IllegalArgumentException("Team not found: " + secondId));
        RivalryAccumulator accumulator = new RivalryAccumulator("TEAM", firstId, secondId, first.getName(), second.getName());
        matchRepository.findCompletedMatchesHistory().stream().filter(m -> plays(first, m) && plays(second, m))
                .forEach(match -> accumulator.add(match, match.getHomeTeam().getId().equals(firstId)));
        RivalryResponse response = accumulator.response();
        return new HeadToHeadResponse("TEAM", firstId, first.getName(), secondId, second.getName(), response.meetings(), response.firstWins(), response.draws(), response.secondWins(), response.firstGoals(), response.secondGoals(), response.firstWins(), response.secondWins());
    }

    private HeadToHeadResponse managerHeadToHead(Long firstId, Long secondId) {
        Manager first = managerRepository.findById(firstId).orElseThrow(() -> new IllegalArgumentException("Manager not found: " + firstId));
        Manager second = managerRepository.findById(secondId).orElseThrow(() -> new IllegalArgumentException("Manager not found: " + secondId));
        List<CareerHistory> histories = careerHistoryRepository.findAll();
        Map<Long, Team> firstTeams = histories.stream().filter(h -> h.getManager().getId().equals(firstId)).collect(Collectors.toMap(h -> h.getTournament().getId(), CareerHistory::getTeam, (a, b) -> a));
        Map<Long, Team> secondTeams = histories.stream().filter(h -> h.getManager().getId().equals(secondId)).collect(Collectors.toMap(h -> h.getTournament().getId(), CareerHistory::getTeam, (a, b) -> a));
        RivalryAccumulator accumulator = new RivalryAccumulator("MANAGER", firstId, secondId, first.getDisplayName(), second.getDisplayName());
        firstTeams.forEach((tournamentId, firstTeam) -> {
            Team secondTeam = secondTeams.get(tournamentId);
            if (secondTeam != null) matchRepository.findByTournamentIdOrderById(tournamentId).stream()
                    .filter(m -> m.getStatus() == MatchStatus.FINISHED && plays(firstTeam, m) && plays(secondTeam, m))
                    .forEach(m -> accumulator.add(m, m.getHomeTeam().getId().equals(firstTeam.getId())));
        });
        RivalryResponse response = accumulator.response();
        return new HeadToHeadResponse("MANAGER", firstId, first.getDisplayName(), secondId, second.getDisplayName(), response.meetings(), response.firstWins(), response.draws(), response.secondWins(), response.firstGoals(), response.secondGoals(), response.firstWins(), response.secondWins());
    }

    private EraAnalysisResponse computeEras() {
        Map<Integer, List<Tournament>> eras = tournamentRepository.findAll().stream()
                .filter(t -> t.getYear() != null).collect(Collectors.groupingBy(t -> (t.getYear() / 4) * 4));
        List<TeamLegacyResponse> teams = teamLegacies();
        List<PlayerLegacyResponse> players = playerLegacies();
        List<ManagerLegacyResponse> managers = managerLegacies();
        String tacticalTrend = analyticsService.getAnalyticsReport().formationAnalytics().stream()
                .max(Comparator.comparingLong(AnalyticsService.FormationAnalytics::usageCount))
                .map(AnalyticsService.FormationAnalytics::formationName).orElse("No persisted tactical trend");
        return new EraAnalysisResponse(eras.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            int from = entry.getKey(); int to = from + 3;
            Set<Long> tournamentIds = entry.getValue().stream().map(Tournament::getId).collect(Collectors.toSet());
            List<Match> matches = matchRepository.findAll().stream().filter(m -> m.getTournament() != null && tournamentIds.contains(m.getTournament().getId()) && m.getStatus() == MatchStatus.FINISHED).toList();
            String bestTeam = bestEraTeam(matches);
            String nation = bestTeam;
            String bestPlayer = bestEraPlayer(tournamentIds, players);
            String manager = managers.stream().max(Comparator.comparingLong(ManagerLegacyResponse::legacyScore)).map(ManagerLegacyResponse::managerName).orElse("None");
            return new EraAnalysisResponse.Era(from + "-" + to, from, to, bestTeam, bestPlayer, manager, nation, tacticalTrend);
        }).toList());
    }

    private HistoricalTimelineResponse computeTimeline() {
        return new HistoricalTimelineResponse(tournamentRepository.findAll().stream()
                .filter(t -> t.getStatus() == TournamentStatus.COMPLETED).sorted(Comparator.comparing(Tournament::getYear))
                .map(tournament -> {
                    TournamentSummaryResponse summary = tournamentSummaryService.summarize(tournament.getId());
                    var intelligence = tournamentIntelligenceService.buildContext(tournament.getId());
                    TournamentAwardsResponse awards = tournamentAwardsService.calculateAwards(tournament.getId());
                    Match finalMatch = matchRepository.findByTournamentIdOrderById(tournament.getId()).stream()
                            .filter(m -> m.getRound() == MatchRound.FINAL && m.getStatus() == MatchStatus.FINISHED).findFirst().orElse(null);
                    String champion = finalMatch == null ? "N/A" : finalMatch.getHomeScore() > finalMatch.getAwayScore() ? finalMatch.getHomeTeam().getName() : finalMatch.getAwayTeam().getName();
                    String runnerUp = finalMatch == null ? "N/A" : finalMatch.getHomeScore() > finalMatch.getAwayScore() ? finalMatch.getAwayTeam().getName() : finalMatch.getHomeTeam().getName();
                    String boot = awards.goldenBoot() == null ? "N/A" : awards.goldenBoot().player();
                    String ball = awards.goldenBall() == null ? "N/A" : awards.goldenBall().player();
                    return new HistoricalTimelineResponse.Entry(tournament.getId(), tournament.getYear(), tournament.getName(), champion, runnerUp,
                            boot, ball, summary.biggestUpset(), intelligence.getCurrentStage() == null
                                    ? summary.highestScoringMatch()
                                    : intelligence.getCurrentStage().name() + ": " + summary.highestScoringMatch());
                }).toList());
    }

    private AwardCounts awardCounts() {
        AwardCounts counts = new AwardCounts();
        tournamentRepository.findAll().stream().filter(t -> t.getStatus() == TournamentStatus.COMPLETED).forEach(tournament -> {
            try {
                TournamentAwardsResponse awards = tournamentAwardsService.calculateAwards(tournament.getId());
                increment(counts.boots, awards.goldenBoot() == null ? null : awards.goldenBoot().playerId());
                increment(counts.balls, awards.goldenBall() == null ? null : awards.goldenBall().playerId());
                increment(counts.gloves, awards.goldenGlove() == null ? null : awards.goldenGlove().playerId());
            } catch (IllegalArgumentException ignored) { }
        });
        return counts;
    }

    private Set<Long> recordHolders() {
        FootballRecordsResponse records = statisticsService.getRecords();
        Set<Long> ids = new HashSet<>();
        if (records.getPlayerRecords() == null) return ids;
        List<List<FootballRecordsResponse.RecordEntry>> groups = List.of(records.getPlayerRecords().getTopScorers(),
                records.getPlayerRecords().getTopAssisters(), records.getPlayerRecords().getMostAppearances(),
                records.getPlayerRecords().getHighestAverageRatings(), records.getPlayerRecords().getYoungestScorers(), records.getPlayerRecords().getOldestScorers());
        groups.stream().filter(Objects::nonNull).flatMap(Collection::stream).map(FootballRecordsResponse.RecordEntry::getId).filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }

    private String bestEraTeam(List<Match> matches) {
        return matches.stream().flatMap(m -> List.of(m.getHomeTeam(), m.getAwayTeam()).stream()).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Team::getName, Collectors.counting())).entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("None");
    }

    private String bestEraPlayer(Set<Long> tournamentIds, List<PlayerLegacyResponse> players) {
        Set<Long> playerIds = ratingRepository.findAll().stream().filter(r -> r.getMatch() != null && r.getMatch().getTournament() != null && tournamentIds.contains(r.getMatch().getTournament().getId()))
                .map(r -> r.getPlayer() == null ? null : r.getPlayer().getId()).filter(Objects::nonNull).collect(Collectors.toSet());
        return players.stream().filter(p -> playerIds.contains(p.playerId())).max(Comparator.comparingLong(PlayerLegacyResponse::legacyScore)).map(PlayerLegacyResponse::playerName).orElse("None");
    }

    private boolean wonByPlayerNation(Player player, Match match) {
        if (player.getCountry() == null || match.getHomeTeam() == null || match.getAwayTeam() == null || match.getHomeTeam().getCountry() == null || match.getAwayTeam().getCountry() == null) return false;
        boolean home = player.getCountry().getId().equals(match.getHomeTeam().getCountry().getId());
        return home ? safe(match.getHomeScore()) > safe(match.getAwayScore()) : safe(match.getAwayScore()) > safe(match.getHomeScore());
    }
    private boolean playerIsHome(Player player, Match match) { return player.getCountry() != null && match.getHomeTeam() != null && match.getHomeTeam().getCountry() != null && player.getCountry().getId().equals(match.getHomeTeam().getCountry().getId()); }
    private boolean plays(Team team, Match match) { return team != null && ((match.getHomeTeam() != null && team.getId().equals(match.getHomeTeam().getId())) || (match.getAwayTeam() != null && team.getId().equals(match.getAwayTeam().getId()))); }
    private boolean won(Team team, Match match) { return score(team, match) > opponentScore(team, match); }
    private int score(Team team, Match match) { return match.getHomeTeam().getId().equals(team.getId()) ? safe(match.getHomeScore()) : safe(match.getAwayScore()); }
    private int opponentScore(Team team, Match match) { return match.getHomeTeam().getId().equals(team.getId()) ? safe(match.getAwayScore()) : safe(match.getHomeScore()); }
    private String nation(Player player) { return player.getCountry() == null ? "Unknown" : player.getCountry().getName(); }
    private int safe(Integer value) { return value == null ? 0 : value; }
    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
    private void increment(Map<Long, Integer> map, Long id) { if (id != null) map.merge(id, 1, Integer::sum); }

    private int streak(Team team, List<Match> matches, Streak type) {
        int current = 0, best = 0;
        for (Match match : matches) {
            int diff = score(team, match) - opponentScore(team, match);
            boolean continues = type == Streak.WINNING ? diff > 0 : diff >= 0;
            current = continues ? current + 1 : 0; best = Math.max(best, current);
        }
        return best;
    }

    private <T> Page<T> page(List<T> values, Pageable pageable, Comparator<T> comparator) {
        List<T> sorted = values.stream().sorted(comparator).toList();
        if (pageable.isUnpaged()) return new PageImpl<>(sorted);
        int start = (int) Math.min(pageable.getOffset(), sorted.size());
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        return new PageImpl<>(sorted.subList(start, end), pageable, sorted.size());
    }
    private <T> List<T> filter(List<T> values, String name, Function<T, String> label) { return name == null || name.isBlank() ? values : values.stream().filter(value -> label.apply(value).toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))).toList(); }
    private <T> String topName(List<T> values, Function<T, Long> score, Function<T, String> name) { return values.stream().max(Comparator.comparing(score)).map(name).orElse("None"); }
    private <T> List<GlobalRankingResponse.Entry> ranking(List<T> values, Function<T, Long> id, Function<T, String> name, Function<T, Long> score, Function<T, String> detail) { List<T> sorted = values.stream().sorted(Comparator.comparing(score).reversed()).toList(); List<GlobalRankingResponse.Entry> result = new ArrayList<>(); for (int i = 0; i < sorted.size(); i++) { T value = sorted.get(i); result.add(new GlobalRankingResponse.Entry(i + 1, id.apply(value), name.apply(value), score.apply(value), detail.apply(value))); } return result; }

    @SuppressWarnings("unchecked") private <T> T cached(String key, java.util.function.Supplier<T> supplier) { CacheEntry entry = cache.get(key); if (entry != null && System.currentTimeMillis() - entry.timestamp < properties.getAnalyticsCacheDurationMs()) return (T) entry.value; T value = supplier.get(); cache.put(key, new CacheEntry(value)); return value; }
    private record CacheEntry(Object value, long timestamp) { private CacheEntry(Object value) { this(value, System.currentTimeMillis()); } }
    private static class AwardCounts { final Map<Long, Integer> boots = new HashMap<>(), balls = new HashMap<>(), gloves = new HashMap<>(); }
    private enum Streak { WINNING, UNBEATEN }
    private static class RivalryAccumulator {
        final String type; final Long firstId, secondId; final String firstName, secondName; int meetings, firstWins, draws, secondWins, firstGoals, secondGoals, biggestVictory, finals;
        RivalryAccumulator(String type, Long firstId, Long secondId, String firstName, String secondName) { this.type = type; this.firstId = firstId; this.secondId = secondId; this.firstName = firstName; this.secondName = secondName; }
        void add(Match match, boolean firstHome) { int first = firstHome ? match.getHomeScore() : match.getAwayScore(); int second = firstHome ? match.getAwayScore() : match.getHomeScore(); meetings++; firstGoals += first; secondGoals += second; if (first > second) firstWins++; else if (first < second) secondWins++; else draws++; biggestVictory = Math.max(biggestVictory, Math.abs(first - second)); if (match.getRound() == MatchRound.FINAL) finals++; }
        RivalryResponse response() { return new RivalryResponse(type, firstId, firstName, secondId, secondName, meetings, firstWins, draws, secondWins, firstGoals, secondGoals, biggestVictory, finals); }
    }
}
