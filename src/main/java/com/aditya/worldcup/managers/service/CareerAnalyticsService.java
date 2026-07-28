package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.CareerAnalyticsResponse;
import com.aditya.worldcup.managers.entity.CareerStatistics;
import com.aditya.worldcup.managers.entity.CoachingStyle;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerCareerAnalytics;
import com.aditya.worldcup.managers.repository.CareerStatisticsRepository;
import com.aditya.worldcup.managers.repository.ManagerCareerAnalyticsRepository;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.repository.TacticalProfileRepository;
import com.aditya.worldcup.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CareerAnalyticsService {

    private static final String UNKNOWN = "Unknown";
    private static final String NONE = "None";

    private final ManagerCareerAnalyticsRepository analyticsRepository;
    private final CareerStatisticsRepository statisticsRepository;
    private final UserRepository userRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final TacticalProfileRepository tacticalProfileRepository;
    private final ManagerService managerService;

    @Transactional
    public CareerAnalyticsResponse getCurrentAnalytics(
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        return mapToResponse(recalculate(manager));
    }

    @Transactional
    public ManagerCareerAnalytics recalculate(Manager manager) {
        CareerStatistics statistics = statisticsRepository
                .findByManagerId(manager.getId())
                .orElse(null);
        List<Squad> squads = findManagerSquads(manager);

        ManagerCareerAnalytics analytics = analyticsRepository
                .findByManagerId(manager.getId())
                .orElseGet(() -> createAnalytics(manager));

        int matchesManaged = statistics == null
                ? 0
                : statistics.getMatchesManaged();
        int goalsScored = statistics == null
                ? 0
                : statistics.getGoalsScored();
        int goalsConceded = statistics == null
                ? 0
                : statistics.getGoalsConceded();

        analytics.setWinPercentage(matchesManaged == 0
                ? 0.0
                : round((statistics.getWins() * 100.0) / matchesManaged));
        analytics.setAverageGoalsScored(matchesManaged == 0
                ? 0.0
                : round(goalsScored / (double) matchesManaged));
        analytics.setAverageGoalsConceded(matchesManaged == 0
                ? 0.0
                : round(goalsConceded / (double) matchesManaged));
        analytics.setAveragePossession(round(averagePossession(squads)));
        analytics.setFavoriteFormation(favoriteFormation(squads));
        analytics.setFavoriteTactics(favoriteTactics(squads));
        analytics.setTacticalProfile(determineTacticalProfile(squads));
        analytics.setMostUsedLineup(mostUsedLineup(squads));
        analytics.setMostSelectedCaptain(mostSelectedCaptain(squads));
        analytics.setMostTrustedPlayers(mostTrustedPlayers(squads));
        analytics.setLongestUnbeatenStreak(longestUnbeatenStreak(squads));
        analytics.setUpdatedAt(LocalDateTime.now());

        return analyticsRepository.save(analytics);
    }

    private List<Squad> findManagerSquads(Manager manager) {
        return userRepository.findByEmail(manager.getUsername())
                .map(user -> squadRepository.findByUserId(user.getId()))
                .orElse(List.of());
    }

    private double averagePossession(List<Squad> squads) {
        List<Double> possessions = squads.stream()
                .flatMap(squad -> matchRepository
                        .findCompletedMatchesByTeamId(squad.getTeam().getId())
                        .stream()
                        .map(match -> new TeamMatch(
                                squad.getTeam().getId(),
                                match)))
                .filter(teamMatch -> teamMatch.match().getStatus()
                        == MatchStatus.FINISHED)
                .map(this::teamPossession)
                .flatMap(Optional::stream)
                .toList();

        return possessions.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private Optional<Double> teamPossession(TeamMatch teamMatch) {
        return matchStatisticsRepository.findByMatchId(teamMatch.match().getId())
                .map(statistics -> {
                    Match match = teamMatch.match();
                    if (match.getHomeTeam() == null
                            || match.getAwayTeam() == null) {
                        return 0.0;
                    }
                    if (match.getHomeTeam().getId()
                            .equals(teamMatch.teamId())) {
                        return safe(statistics.getHomePossession())
                                .doubleValue();
                    }
                    return safe(statistics.getAwayPossession()).doubleValue();
                });
    }

    private String favoriteFormation(List<Squad> squads) {
        return mostCommon(
                squads.stream()
                        .filter(squad -> squad.getFormation() != null)
                        .map(squad -> squad.getFormation().getName())
                        .toList(),
                UNKNOWN
        );
    }

    private String favoriteTactics(List<Squad> squads) {
        return mostCommon(
                squads.stream()
                        .map(squad -> tacticalProfileRepository
                                .findByTeamId(squad.getTeam().getId()))
                        .flatMap(Optional::stream)
                        .map(this::describeTactics)
                        .toList(),
                "Balanced"
        );
    }

    private CoachingStyle determineTacticalProfile(List<Squad> squads) {
        List<TacticalProfile> profiles = squads.stream()
                .map(squad -> tacticalProfileRepository
                        .findByTeamId(squad.getTeam().getId()))
                .flatMap(Optional::stream)
                .toList();

        if (profiles.isEmpty()) {
            return CoachingStyle.BALANCED;
        }

        double pressing = profiles.stream()
                .mapToInt(TacticalProfile::getPressingIntensity)
                .average()
                .orElse(50);
        double line = profiles.stream()
                .mapToInt(TacticalProfile::getDefensiveLine)
                .average()
                .orElse(50);
        double risk = profiles.stream()
                .mapToInt(TacticalProfile::getPassingRisk)
                .average()
                .orElse(50);
        double width = profiles.stream()
                .mapToInt(TacticalProfile::getAttackingWidth)
                .average()
                .orElse(50);
        long counters = profiles.stream()
                .filter(TacticalProfile::getCounterAttack)
                .count();
        long highPress = profiles.stream()
                .filter(TacticalProfile::getHighPress)
                .count();

        if (highPress > profiles.size() / 2 || pressing >= 65) {
            return CoachingStyle.HIGH_PRESS;
        }
        if (counters > profiles.size() / 2) {
            return CoachingStyle.COUNTER_ATTACKING;
        }
        if (risk >= 60 || width >= 60) {
            return CoachingStyle.ATTACKING;
        }
        if (line <= 40 || pressing <= 40) {
            return CoachingStyle.DEFENSIVE;
        }
        if (risk <= 45 && pressing <= 55) {
            return CoachingStyle.POSSESSION;
        }
        return CoachingStyle.BALANCED;
    }

    private String mostUsedLineup(List<Squad> squads) {
        List<String> lineups = squads.stream()
                .map(squad -> squadPlayerRepository
                        .findBySquadIdAndStartingXiTrue(squad.getId())
                        .stream()
                        .map(sp -> sp.getPlayer().getName())
                        .sorted()
                        .collect(Collectors.joining(", ")))
                .filter(lineup -> !lineup.isBlank())
                .toList();

        return mostCommon(lineups, NONE);
    }

    private String mostSelectedCaptain(List<Squad> squads) {
        return mostCommon(
                squads.stream()
                        .flatMap(squad -> squadPlayerRepository
                                .findBySquadId(squad.getId())
                                .stream())
                        .filter(SquadPlayer::getCaptain)
                        .map(sp -> sp.getPlayer().getName())
                        .toList(),
                NONE
        );
    }

    private String mostTrustedPlayers(List<Squad> squads) {
        String players = squads.stream()
                .flatMap(squad -> squadPlayerRepository
                        .findBySquadId(squad.getId())
                        .stream())
                .filter(SquadPlayer::getStartingXi)
                .map(sp -> sp.getPlayer().getName())
                .distinct()
                .sorted()
                .limit(5)
                .collect(Collectors.joining(", "));

        return players.isBlank() ? NONE : players;
    }

    private int longestUnbeatenStreak(List<Squad> squads) {
        return squads.stream()
                .mapToInt(squad -> longestUnbeatenStreakForTeam(
                        squad.getTeam().getId()))
                .max()
                .orElse(0);
    }

    private int longestUnbeatenStreakForTeam(Long teamId) {
        List<Match> matches = matchRepository.findCompletedMatchesByTeamId(teamId)
                .stream()
                .sorted(Comparator.comparing(Match::getMatchDate)
                        .thenComparing(Match::getId))
                .toList();
        int current = 0;
        int best = 0;

        for (Match match : matches) {
            if (isLoss(match, teamId)) {
                current = 0;
            } else {
                current++;
                best = Math.max(best, current);
            }
        }

        return best;
    }

    private boolean isLoss(Match match, Long teamId) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            return false;
        }
        if (match.getHomeTeam().getId().equals(teamId)) {
            return match.getHomeScore() < match.getAwayScore();
        }
        return match.getAwayScore() < match.getHomeScore();
    }

    private Integer safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String describeTactics(TacticalProfile profile) {
        return determineTacticalProfileForProfile(profile).name();
    }

    private CoachingStyle determineTacticalProfileForProfile(
            TacticalProfile profile) {

        if (Boolean.TRUE.equals(profile.getHighPress())
                || profile.getPressingIntensity() >= 65) {
            return CoachingStyle.HIGH_PRESS;
        }
        if (Boolean.TRUE.equals(profile.getCounterAttack())) {
            return CoachingStyle.COUNTER_ATTACKING;
        }
        if (profile.getPassingRisk() >= 60
                || profile.getAttackingWidth() >= 60) {
            return CoachingStyle.ATTACKING;
        }
        if (profile.getDefensiveLine() <= 40
                || profile.getPressingIntensity() <= 40) {
            return CoachingStyle.DEFENSIVE;
        }
        if (profile.getPassingRisk() <= 45) {
            return CoachingStyle.POSSESSION;
        }
        return CoachingStyle.BALANCED;
    }

    private String mostCommon(List<String> values, String fallback) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(fallback);
    }

    private ManagerCareerAnalytics createAnalytics(Manager manager) {
        return ManagerCareerAnalytics.builder()
                .manager(manager)
                .winPercentage(0.0)
                .averageGoalsScored(0.0)
                .averageGoalsConceded(0.0)
                .averagePossession(0.0)
                .favoriteFormation(UNKNOWN)
                .favoriteTactics("Balanced")
                .tacticalProfile(CoachingStyle.BALANCED)
                .mostUsedLineup(NONE)
                .mostSelectedCaptain(NONE)
                .mostTrustedPlayers(NONE)
                .longestUnbeatenStreak(0)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CareerAnalyticsResponse mapToResponse(
            ManagerCareerAnalytics analytics) {

        return new CareerAnalyticsResponse(
                analytics.getManager().getId(),
                analytics.getWinPercentage(),
                analytics.getAverageGoalsScored(),
                analytics.getAverageGoalsConceded(),
                analytics.getAveragePossession(),
                analytics.getFavoriteFormation(),
                analytics.getFavoriteTactics(),
                analytics.getTacticalProfile(),
                analytics.getMostUsedLineup(),
                analytics.getMostSelectedCaptain(),
                analytics.getMostTrustedPlayers(),
                analytics.getLongestUnbeatenStreak()
        );
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record TeamMatch(
            Long teamId,
            Match match
    ) {
    }
}
