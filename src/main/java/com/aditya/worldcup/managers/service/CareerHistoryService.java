package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.CareerHistoryResponse;
import com.aditya.worldcup.managers.entity.CareerHistory;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.CareerHistoryRepository;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CareerHistoryService {

    private final CareerHistoryRepository careerHistoryRepository;
    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final ManagerService managerService;
    private final CareerStatisticsService careerStatisticsService;

    @Transactional
    public List<CareerHistoryResponse> getCurrentHistory(
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        return careerHistoryRepository
                .findByManagerIdOrderByDateCompletedDesc(manager.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void recordCompletedTournament(Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return;
        }

        List<Match> matches = matchRepository
                .findByTournamentIdOrderById(tournament.getId())
                .stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .toList();

        if (matches.isEmpty()) {
            return;
        }

        Match finalMatch = findFirstRoundMatch(matches, MatchRound.FINAL);
        Team champion = finalMatch == null ? null : determineWinner(finalMatch);
        Team finalist = finalMatch == null ? null : determineLoser(finalMatch);
        List<Team> semiFinalTeams = teamsInRound(matches, MatchRound.SEMI_FINALS);
        List<Team> knockoutTeams = matches.stream()
                .filter(match -> match.getRound() != MatchRound.GROUP_STAGE)
                .flatMap(match -> Stream.of(
                        match.getHomeTeam(),
                        match.getAwayTeam()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Team> managedTeams = matches.stream()
                .flatMap(match -> Stream.of(
                        match.getHomeTeam(),
                        match.getAwayTeam()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        for (Team team : managedTeams) {
            squadRepository.findFirstByTeamId(team.getId())
                    .ifPresent(squad -> recordTeamHistory(
                            tournament,
                            matches,
                            squad,
                            team,
                            champion,
                            finalist,
                            semiFinalTeams,
                            knockoutTeams
                    ));
        }
    }

    private void recordTeamHistory(
            Tournament tournament,
            List<Match> matches,
            Squad squad,
            Team team,
            Team champion,
            Team finalist,
            List<Team> semiFinalTeams,
            List<Team> knockoutTeams
    ) {
        Manager manager = managerService.getOrCreateManager(
                squad.getUser().getEmail());

        if (careerHistoryRepository.existsByManagerIdAndTournamentIdAndTeamId(
                manager.getId(),
                tournament.getId(),
                team.getId())) {
            return;
        }

        TeamTournamentRecord record = buildRecord(matches, team);
        boolean tournamentVictory = sameTeam(team, champion);
        boolean reachedFinal = tournamentVictory || sameTeam(team, finalist);
        boolean reachedSemiFinal = semiFinalTeams.stream()
                .anyMatch(semiFinalTeam -> sameTeam(team, semiFinalTeam));
        boolean reachedKnockout = knockoutTeams.stream()
                .anyMatch(knockoutTeam -> sameTeam(team, knockoutTeam));

        CareerHistory history = CareerHistory.builder()
                .manager(manager)
                .tournament(tournament)
                .team(team)
                .finishingPosition(finishingPosition(
                        tournamentVictory,
                        reachedFinal,
                        reachedSemiFinal,
                        reachedKnockout
                ))
                .wins(record.wins())
                .losses(record.losses())
                .goalsScored(record.goalsScored())
                .goalsConceded(record.goalsConceded())
                .trophies(tournamentVictory ? 1 : 0)
                .dateCompleted(LocalDateTime.now())
                .build();

        careerHistoryRepository.save(history);
        careerStatisticsService.recordCompletedTournamentTeam(
                manager,
                team,
                tournament.getId(),
                reachedKnockout,
                reachedFinal,
                reachedSemiFinal,
                tournamentVictory,
                record.losses() == 0,
                record.wins(),
                record.goalsScored(),
                record.goalsConceded(),
                knockoutWins(matches, team)
        );
    }

    private TeamTournamentRecord buildRecord(List<Match> matches, Team team) {
        int wins = 0;
        int losses = 0;
        int goalsScored = 0;
        int goalsConceded = 0;

        for (Match match : matches) {
            if (match.getHomeTeam() != null
                    && match.getHomeTeam().getId().equals(team.getId())) {
                goalsScored += match.getHomeScore();
                goalsConceded += match.getAwayScore();
                if (match.getHomeScore() > match.getAwayScore()) {
                    wins++;
                } else if (match.getHomeScore() < match.getAwayScore()) {
                    losses++;
                }
            } else if (match.getAwayTeam() != null
                    && match.getAwayTeam().getId().equals(team.getId())) {
                goalsScored += match.getAwayScore();
                goalsConceded += match.getHomeScore();
                if (match.getAwayScore() > match.getHomeScore()) {
                    wins++;
                } else if (match.getAwayScore() < match.getHomeScore()) {
                    losses++;
                }
            }
        }

        return new TeamTournamentRecord(
                wins,
                losses,
                goalsScored,
                goalsConceded
        );
    }

    private Match findFirstRoundMatch(
            List<Match> matches,
            MatchRound round) {

        return matches.stream()
                .filter(match -> match.getRound() == round)
                .min(Comparator.comparing(Match::getId))
                .orElse(null);
    }

    private List<Team> teamsInRound(
            List<Match> matches,
            MatchRound round) {

        return matches.stream()
                .filter(match -> match.getRound() == round)
                .flatMap(match -> Stream.of(
                        match.getHomeTeam(),
                        match.getAwayTeam()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Team determineWinner(Match match) {
        if (match.getHomeScore() > match.getAwayScore()) {
            return match.getHomeTeam();
        }
        return match.getAwayTeam();
    }

    private Team determineLoser(Match match) {
        if (match.getHomeScore() > match.getAwayScore()) {
            return match.getAwayTeam();
        }
        return match.getHomeTeam();
    }

    private int finishingPosition(
            boolean tournamentVictory,
            boolean reachedFinal,
            boolean reachedSemiFinal,
            boolean reachedKnockout
    ) {
        if (tournamentVictory) {
            return 1;
        }
        if (reachedFinal) {
            return 2;
        }
        if (reachedSemiFinal) {
            return 3;
        }
        if (reachedKnockout) {
            return 8;
        }
        return 16;
    }

    private boolean sameTeam(Team first, Team second) {
        return first != null
                && second != null
                && first.getId().equals(second.getId());
    }

    private int knockoutWins(List<Match> matches, Team team) {
        return (int) matches.stream()
                .filter(match -> match.getRound() != MatchRound.GROUP_STAGE)
                .filter(match -> sameTeam(team, determineWinner(match)))
                .count();
    }

    private CareerHistoryResponse mapToResponse(CareerHistory history) {
        return new CareerHistoryResponse(
                history.getId(),
                history.getTournament().getId(),
                history.getTournament().getName(),
                history.getTeam().getId(),
                history.getTeam().getName(),
                history.getFinishingPosition(),
                history.getWins(),
                history.getLosses(),
                history.getGoalsScored(),
                history.getGoalsConceded(),
                history.getTrophies(),
                history.getDateCompleted()
        );
    }

    private record TeamTournamentRecord(
            int wins,
            int losses,
            int goalsScored,
            int goalsConceded
    ) {
    }
}
