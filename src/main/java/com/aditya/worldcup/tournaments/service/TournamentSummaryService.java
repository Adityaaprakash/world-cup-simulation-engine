package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchevents.entity.MatchEventType;
import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.dto.MatchNarrativeResponse;
import com.aditya.worldcup.tournaments.dto.TournamentSummaryResponse;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TournamentSummaryService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final TournamentIntelligenceService tournamentIntelligenceService;
    private final MatchNarrativeService matchNarrativeService;
    private final TournamentTeamAwardsService tournamentTeamAwardsService;

    @Transactional(readOnly = true)
    public TournamentSummaryResponse summarize(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        TournamentContext context = tournamentIntelligenceService.buildContext(tournamentId);
        List<Match> completedMatches = matchRepository.findByTournamentIdOrderById(tournamentId)
                .stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .filter(match -> match.getHomeScore() != null && match.getAwayScore() != null)
                .toList();
        List<MatchNarrativeResponse> narratives = completedMatches.stream()
                .map(matchNarrativeService::generate)
                .toList();

        return new TournamentSummaryResponse(
                tournamentId,
                context.getCurrentStage() == null ? null : context.getCurrentStage().name(),
                context.getRemainingFixtures(),
                upsetLabel(context.getBiggestUpset()),
                matchLabel(mostEntertainingMatch(completedMatches)),
                matchLabel(highestScoringMatch(completedMatches)),
                topScorer(completedMatches),
                bestGoalkeeper(completedMatches),
                championPath(completedMatches),
                longestStreak(context),
                totalGoals(completedMatches),
                completedMatches.size(),
                narratives,
                tournamentTeamAwardsService.calculate(tournamentId)
        );
    }

    private Match highestScoringMatch(List<Match> matches) {
        return matches.stream()
                .max(Comparator.comparingInt(this::goalTotal))
                .orElse(null);
    }

    private Match mostEntertainingMatch(List<Match> matches) {
        return matches.stream()
                .max(Comparator.comparingDouble(this::entertainmentScore))
                .orElse(null);
    }

    private double entertainmentScore(Match match) {
        double score = goalTotal(match) * 2.0;
        score += Math.abs(match.getHomeScore() - match.getAwayScore()) <= 1 ? 1.5 : 0.0;
        score += matchStatisticsRepository.findByMatchId(match.getId())
                .map(statistics -> (statistics.getHomeShots() + statistics.getAwayShots()) * 0.08
                        + (statistics.getHomeExpectedGoals() + statistics.getAwayExpectedGoals()) * 0.4)
                .orElse(0.0);
        score += matchEventRepository.findByMatchId(match.getId())
                .stream()
                .filter(event -> event.getMinute() != null && event.getMinute() >= 76)
                .count() * 0.5;
        return score;
    }

    private String topScorer(List<Match> matches) {
        Map<Long, PlayerGoalStat> stats = new LinkedHashMap<>();
        matches.forEach(match -> matchEventRepository.findByMatchId(match.getId())
                .stream()
                .filter(event -> event.getEventType() == MatchEventType.GOAL)
                .filter(event -> event.getPlayer() != null)
                .forEach(event -> stats.computeIfAbsent(
                        event.getPlayer().getId(),
                        ignored -> new PlayerGoalStat(event.getPlayer()))
                        .goals++));
        return stats.values()
                .stream()
                .max(Comparator.comparingInt((PlayerGoalStat stat) -> stat.goals)
                        .thenComparing(stat -> -stat.player.getId()))
                .map(stat -> stat.player.getName() + " (" + stat.goals + " goals)")
                .orElse(null);
    }

    private String bestGoalkeeper(List<Match> matches) {
        Map<Long, GoalkeeperStat> stats = new LinkedHashMap<>();
        matches.forEach(match -> playerMatchRatingRepository.findByMatchId(match.getId())
                .stream()
                .map(PlayerMatchRating::getPlayer)
                .filter(Objects::nonNull)
                .filter(player -> player.getPosition() == PlayerPosition.GK)
                .forEach(player -> addGoalkeeperRating(stats, player, match)));
        return stats.values()
                .stream()
                .max(Comparator.comparingDouble(GoalkeeperStat::score))
                .map(stat -> stat.player.getName())
                .orElse(null);
    }

    private void addGoalkeeperRating(Map<Long, GoalkeeperStat> stats,
                                     Player player,
                                     Match match) {
        GoalkeeperStat stat = stats.computeIfAbsent(
                player.getId(),
                ignored -> new GoalkeeperStat(player));
        playerMatchRatingRepository.findByMatchId(match.getId())
                .stream()
                .filter(rating -> rating.getPlayer().getId().equals(player.getId()))
                .findFirst()
                .ifPresent(rating -> {
                    stat.matches++;
                    stat.ratingTotal += rating.getRating();
                    Integer conceded = goalsConceded(player, match);
                    if (conceded != null && conceded == 0) {
                        stat.cleanSheets++;
                    }
                });
    }

    private Integer goalsConceded(Player player, Match match) {
        if (player.getCountry().getId().equals(match.getHomeTeam().getCountry().getId())) {
            return match.getAwayScore();
        }
        if (player.getCountry().getId().equals(match.getAwayTeam().getCountry().getId())) {
            return match.getHomeScore();
        }
        return null;
    }

    private String championPath(List<Match> matches) {
        Match finalMatch = matches.stream()
                .filter(match -> match.getRound() != null
                        && "FINAL".equals(match.getRound().name()))
                .findFirst()
                .orElse(null);
        if (finalMatch == null || finalMatch.getHomeScore().equals(finalMatch.getAwayScore())) {
            return null;
        }
        Team champion = finalMatch.getHomeScore() > finalMatch.getAwayScore()
                ? finalMatch.getHomeTeam()
                : finalMatch.getAwayTeam();
        return matches.stream()
                .filter(match -> match.getHomeTeam().getId().equals(champion.getId())
                        || match.getAwayTeam().getId().equals(champion.getId()))
                .map(this::matchLabel)
                .collect(java.util.stream.Collectors.joining(" -> "));
    }

    private String longestStreak(TournamentContext context) {
        Team team = context.getLongestUnbeatenStreakTeam();
        if (team == null) {
            return null;
        }
        TeamTournamentProfile profile = context.getTeamProfiles().get(team.getId());
        return team.getName() + " unbeaten in "
                + profile.getLongestUnbeatenStreak() + " matches";
    }

    private int totalGoals(List<Match> matches) {
        return matches.stream().mapToInt(this::goalTotal).sum();
    }

    private int goalTotal(Match match) {
        return match.getHomeScore() + match.getAwayScore();
    }

    private String matchLabel(Match match) {
        if (match == null) {
            return null;
        }
        return match.getHomeTeam().getName() + " " + match.getHomeScore()
                + "-" + match.getAwayScore() + " " + match.getAwayTeam().getName();
    }

    private String upsetLabel(TournamentUpset upset) {
        if (upset == null) {
            return null;
        }
        return upset.winner().getName() + " beat " + upset.loser().getName()
                + " at " + upset.stage() + " despite a "
                + upset.ratingDifference() + "-point rating gap";
    }

    private static class PlayerGoalStat {

        private final Player player;
        private int goals;

        private PlayerGoalStat(Player player) {
            this.player = player;
        }
    }

    private static class GoalkeeperStat {

        private final Player player;
        private int matches;
        private int cleanSheets;
        private double ratingTotal;

        private GoalkeeperStat(Player player) {
            this.player = player;
        }

        private double score() {
            return matches == 0 ? 0.0 : ratingTotal / matches + cleanSheets * 0.2;
        }
    }
}
