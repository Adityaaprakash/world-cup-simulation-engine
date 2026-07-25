package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TournamentIntelligenceService {

    private static final int MAJOR_UPSET_RATING_DIFFERENCE = 8;

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchRepository matchRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerStateService playerStateService;

    @Transactional(readOnly = true)
    public TournamentContext buildContext(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        List<Team> teams = tournamentTeamRepository.findByTournamentId(tournamentId)
                .stream()
                .map(TournamentTeam::getTeam)
                .toList();
        List<Match> matches = matchRepository.findByTournamentIdOrderById(tournamentId);
        List<Match> completedMatches = matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .filter(match -> match.getHomeScore() != null && match.getAwayScore() != null)
                .toList();

        Map<Long, TeamTournamentProfile> profiles = new LinkedHashMap<>();
        teams.forEach(team -> profiles.put(team.getId(),
                new TeamTournamentProfile(team, classify(team, teams))));

        TournamentContext context = new TournamentContext();
        context.setTeamProfiles(profiles);
        context.setRemainingFixtures(matches.stream()
                .filter(match -> match.getStatus() != MatchStatus.FINISHED)
                .count());
        context.setCurrentStage(currentStage(matches));
        context.setKnockoutStage(context.getCurrentStage() != null
                && context.getCurrentStage() != MatchRound.GROUP_STAGE);
        context.setFavourites(favourites(teams));

        for (Match match : completedMatches) {
            applyMatch(match, profiles, context);
        }

        context.setDarkHorses(darkHorses(profiles));
        context.setHighestScoringTeam(maxProfile(profiles, TeamTournamentProfile::getGoalsFor));
        context.setBestDefensiveTeam(minProfile(profiles, TeamTournamentProfile::getGoalsAgainst));
        context.setLongestWinningStreakTeam(maxProfile(profiles, TeamTournamentProfile::getLongestWinningStreak));
        context.setLongestUnbeatenStreakTeam(maxProfile(profiles, TeamTournamentProfile::getLongestUnbeatenStreak));
        context.setLongestCleanSheetStreakTeam(maxProfile(profiles, TeamTournamentProfile::getLongestCleanSheetStreak));
        context.setLongestLossStreakTeam(maxProfile(profiles, TeamTournamentProfile::getLongestLossStreak));
        return context;
    }

    @Transactional(readOnly = true)
    public double momentumForTeam(Long tournamentId, Long teamId) {
        TeamTournamentProfile profile = buildContext(tournamentId)
                .getTeamProfiles()
                .get(teamId);
        return profile == null ? 0.0 : profile.getMomentum();
    }

    @Transactional(readOnly = true)
    public String formForTeam(Long tournamentId, Long teamId) {
        TeamTournamentProfile profile = buildContext(tournamentId)
                .getTeamProfiles()
                .get(teamId);
        return profile == null ? "" : profile.getForm();
    }

    @Transactional(readOnly = true)
    public TeamReputation reputationForTeam(Long tournamentId, Long teamId) {
        TeamTournamentProfile profile = buildContext(tournamentId)
                .getTeamProfiles()
                .get(teamId);
        return profile == null ? TeamReputation.OUTSIDER : profile.getReputation();
    }

    @Transactional
    public void applyCompletedMatchEffects(Match match) {
        if (match == null || match.getTournament() == null
                || match.getHomeScore() == null || match.getAwayScore() == null) {
            return;
        }
        TournamentContext context = buildContext(match.getTournament().getId());
        applyTeamConfidence(match.getHomeTeam(),
                confidenceAdjustment(match, match.getHomeTeam(), context));
        applyTeamConfidence(match.getAwayTeam(),
                confidenceAdjustment(match, match.getAwayTeam(), context));
    }

    private void applyMatch(Match match,
                            Map<Long, TeamTournamentProfile> profiles,
                            TournamentContext context) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();
        TeamTournamentProfile homeProfile = profiles.get(home.getId());
        TeamTournamentProfile awayProfile = profiles.get(away.getId());
        if (homeProfile == null || awayProfile == null) {
            return;
        }

        int homeGoals = match.getHomeScore();
        int awayGoals = match.getAwayScore();
        homeProfile.applyResult(homeGoals, awayGoals,
                home.getOverallRating() - away.getOverallRating());
        awayProfile.applyResult(awayGoals, homeGoals,
                away.getOverallRating() - home.getOverallRating());

        if (match.getRound() != MatchRound.GROUP_STAGE && homeGoals != awayGoals) {
            TeamTournamentProfile winner = homeGoals > awayGoals ? homeProfile : awayProfile;
            winner.addMomentum(0.35 + knockoutPressure(match.getRound()));
        }

        TournamentUpset upset = detectUpset(match);
        if (upset != null && (context.getBiggestUpset() == null
                || upset.ratingDifference() > context.getBiggestUpset().ratingDifference())) {
            context.setBiggestUpset(upset);
        }
    }

    private TournamentUpset detectUpset(Match match) {
        if (match.getHomeScore().equals(match.getAwayScore())) {
            return null;
        }
        Team winner = match.getHomeScore() > match.getAwayScore()
                ? match.getHomeTeam()
                : match.getAwayTeam();
        Team loser = winner.getId().equals(match.getHomeTeam().getId())
                ? match.getAwayTeam()
                : match.getHomeTeam();
        int difference = loser.getOverallRating() - winner.getOverallRating();
        if (difference < MAJOR_UPSET_RATING_DIFFERENCE) {
            return null;
        }
        return new TournamentUpset(winner, loser, match.getRound(), difference);
    }

    private int confidenceAdjustment(Match match,
                                     Team team,
                                     TournamentContext context) {
        Team opponent = team.getId().equals(match.getHomeTeam().getId())
                ? match.getAwayTeam()
                : match.getHomeTeam();
        int goalsFor = team.getId().equals(match.getHomeTeam().getId())
                ? match.getHomeScore()
                : match.getAwayScore();
        int goalsAgainst = team.getId().equals(match.getHomeTeam().getId())
                ? match.getAwayScore()
                : match.getHomeScore();
        TeamTournamentProfile profile = context.getTeamProfiles().get(team.getId());
        int adjustment = 0;
        if (goalsFor > goalsAgainst) {
            adjustment += 1;
            if (opponent.getOverallRating() - team.getOverallRating()
                    >= MAJOR_UPSET_RATING_DIFFERENCE) {
                adjustment += 2;
            }
            if (profile != null && profile.getWinningStreak() >= 3) {
                adjustment += 1;
            }
        } else if (goalsAgainst - goalsFor >= 3) {
            adjustment -= 2;
        } else if (profile != null && profile.getLossStreak() >= 2) {
            adjustment -= 1;
        }
        return adjustment;
    }

    private void applyTeamConfidence(Team team, int adjustment) {
        if (adjustment == 0) {
            return;
        }
        squadPlayerRepository.findBySquadTeamId(team.getId())
                .stream()
                .map(SquadPlayer::getPlayer)
                .map(playerStateService::getOrCreateState)
                .forEach(state -> state.setConfidence(
                        clamp(state.getConfidence() + adjustment, 0, 100)));
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private MatchRound currentStage(List<Match> matches) {
        return matches.stream()
                .filter(match -> match.getStatus() != MatchStatus.FINISHED)
                .map(Match::getRound)
                .findFirst()
                .orElseGet(() -> matches.stream()
                        .map(Match::getRound)
                        .max(Comparator.comparingInt(this::roundOrder))
                        .orElse(MatchRound.GROUP_STAGE));
    }

    private int roundOrder(MatchRound round) {
        return switch (round) {
            case GROUP_STAGE -> 0;
            case ROUND_OF_16 -> 1;
            case QUARTER_FINALS -> 2;
            case SEMI_FINALS -> 3;
            case FINAL -> 4;
        };
    }

    private TeamReputation classify(Team team, List<Team> teams) {
        double average = teams.stream().mapToInt(Team::getOverallRating).average().orElse(80);
        int rating = team.getOverallRating();
        if (rating >= average + 7) {
            return TeamReputation.FAVOURITE;
        }
        if (rating >= average + 2) {
            return TeamReputation.CONTENDER;
        }
        if (rating >= average - 5) {
            return TeamReputation.OUTSIDER;
        }
        return TeamReputation.UNDERDOG;
    }

    private List<Team> favourites(List<Team> teams) {
        return teams.stream()
                .sorted(Comparator.comparingInt(Team::getOverallRating).reversed())
                .limit(Math.max(1, Math.min(4, teams.size())))
                .toList();
    }

    private List<Team> darkHorses(Map<Long, TeamTournamentProfile> profiles) {
        return profiles.values()
                .stream()
                .filter(profile -> profile.getReputation() == TeamReputation.OUTSIDER
                        || profile.getReputation() == TeamReputation.UNDERDOG)
                .filter(profile -> profile.getMomentum() > 0.6
                        || profile.getUnbeatenStreak() >= 2)
                .sorted(Comparator.comparingDouble(TeamTournamentProfile::getMomentum).reversed())
                .map(TeamTournamentProfile::getTeam)
                .limit(3)
                .toList();
    }

    private double knockoutPressure(MatchRound round) {
        return switch (round) {
            case QUARTER_FINALS -> 0.2;
            case SEMI_FINALS -> 0.35;
            case FINAL -> 0.5;
            default -> 0.1;
        };
    }

    private Team maxProfile(Map<Long, TeamTournamentProfile> profiles,
                            java.util.function.ToIntFunction<TeamTournamentProfile> value) {
        return profiles.values()
                .stream()
                .max(Comparator.comparingInt(value))
                .map(TeamTournamentProfile::getTeam)
                .orElse(null);
    }

    private Team minProfile(Map<Long, TeamTournamentProfile> profiles,
                            java.util.function.ToIntFunction<TeamTournamentProfile> value) {
        return profiles.values()
                .stream()
                .min(Comparator.comparingInt(value))
                .map(TeamTournamentProfile::getTeam)
                .orElse(null);
    }
}
