package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.dto.CareerStatisticsResponse;
import com.aditya.worldcup.managers.entity.CareerStatistics;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.TimelineEventType;
import com.aditya.worldcup.managers.repository.CareerStatisticsRepository;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CareerStatisticsService {

    private final CareerStatisticsRepository careerStatisticsRepository;
    private final SquadRepository squadRepository;
    private final ManagerService managerService;
    private final CareerProgressionService careerProgressionService;
    private final AchievementService achievementService;
    private final CareerAnalyticsService careerAnalyticsService;
    private final CareerTimelineService timelineService;

    @Transactional
    public CareerStatisticsResponse getCurrentStatistics(
            Authentication authentication) {

        Manager manager = managerService.getOrCreateManager(authentication);
        return mapToResponse(getOrCreateStatistics(manager));
    }

    @Transactional
    public void recordCompletedMatch(Match match) {
        if (match == null
                || match.getStatus() != MatchStatus.FINISHED
                || match.getHomeTeam() == null
                || match.getAwayTeam() == null
                || match.getHomeScore() == null
                || match.getAwayScore() == null) {
            return;
        }

        recordTeamMatch(
                match,
                match.getHomeTeam(),
                match.getHomeScore(),
                match.getAwayScore()
        );
        recordTeamMatch(
                match,
                match.getAwayTeam(),
                match.getAwayScore(),
                match.getHomeScore()
        );
    }

    @Transactional
    public void recordCompletedTournamentTeam(
            Manager manager,
            Team team,
            Long tournamentId,
            boolean reachedKnockout,
            boolean reachedFinal,
            boolean reachedSemiFinal,
            boolean tournamentVictory,
            boolean invincible,
            int tournamentWins,
            int tournamentGoalsScored,
            int tournamentGoalsConceded,
            int knockoutWins
    ) {
        CareerStatistics statistics = getOrCreateStatistics(manager);
        statistics.setTournamentsManaged(statistics.getTournamentsManaged() + 1);

        if (reachedSemiFinal) {
            statistics.setSemiFinalsReached(
                    statistics.getSemiFinalsReached() + 1);
        }

        if (reachedFinal) {
            statistics.setFinalsReached(statistics.getFinalsReached() + 1);
        }

        if (tournamentVictory) {
            statistics.setTrophiesWon(statistics.getTrophiesWon() + 1);
        }

        statistics.setUpdatedAt(LocalDateTime.now());
        careerStatisticsRepository.save(statistics);

        ManagerService.ProgressionResult progression =
                managerService.addExperience(
                manager,
                careerProgressionService.calculateExperience(
                        false,
                        false,
                        reachedKnockout,
                        reachedFinal,
                        tournamentVictory,
                        0
                )
        );
        recordProgressionTimeline(manager, progression);

        timelineService.recordEvent(
                manager,
                tournamentVictory
                        ? TimelineEventType.TROPHY
                        : TimelineEventType.TOURNAMENT,
                tournamentVictory
                        ? "Tournament trophy won"
                        : "Tournament completed",
                team.getName() + " finished position updated with "
                        + tournamentWins + " tournament wins.",
                tournamentId,
                team.getId()
        );
        achievementService.evaluateAfterTournament(
                manager,
                tournamentVictory,
                invincible,
                statistics.getTournamentsManaged(),
                tournamentGoalsScored,
                tournamentGoalsConceded,
                knockoutWins
        );
        careerAnalyticsService.recalculate(manager);
    }

    private void recordTeamMatch(
            Match match,
            Team team,
            int goalsFor,
            int goalsAgainst
    ) {
        Optional<Squad> squad = squadRepository.findFirstByTeamId(team.getId());
        if (squad.isEmpty()) {
            return;
        }

        Manager manager = managerService.getOrCreateManager(
                squad.get().getUser().getEmail());
        CareerStatistics statistics = getOrCreateStatistics(manager);

        statistics.setMatchesManaged(statistics.getMatchesManaged() + 1);
        statistics.setGoalsScored(statistics.getGoalsScored() + goalsFor);
        statistics.setGoalsConceded(
                statistics.getGoalsConceded() + goalsAgainst);

        boolean victory = goalsFor > goalsAgainst;
        boolean draw = goalsFor == goalsAgainst;

        if (victory) {
            statistics.setWins(statistics.getWins() + 1);
        } else if (draw) {
            statistics.setDraws(statistics.getDraws() + 1);
        } else {
            statistics.setLosses(statistics.getLosses() + 1);
        }

        if (goalsAgainst == 0) {
            statistics.setCleanSheets(statistics.getCleanSheets() + 1);
        }

        statistics.setUpdatedAt(LocalDateTime.now());
        careerStatisticsRepository.save(statistics);

        if (statistics.getMatchesManaged() == 1) {
            timelineService.recordEvent(
                    manager,
                    TimelineEventType.MILESTONE,
                    "First match managed",
                    "Managed the first match of the career.",
                    match.getTournament() == null
                            ? null
                            : match.getTournament().getId(),
                    team.getId()
            );
        }

        if (victory && statistics.getWins() == 1) {
            timelineService.recordEvent(
                    manager,
                    TimelineEventType.MILESTONE,
                    "First career win",
                    "Recorded the first win of the career.",
                    match.getTournament() == null
                            ? null
                            : match.getTournament().getId(),
                    team.getId()
            );
        }

        ManagerService.ProgressionResult progression =
                managerService.addExperience(
                manager,
                careerProgressionService.calculateExperience(
                        victory,
                        draw,
                        false,
                        false,
                        false,
                        0
                )
        );
        recordProgressionTimeline(manager, progression);

        achievementService.evaluateAfterMatch(
                manager,
                match,
                team,
                statistics.getWins(),
                statistics.getCleanSheets(),
                goalsFor,
                goalsAgainst
        );
        careerAnalyticsService.recalculate(manager);
    }

    private void recordProgressionTimeline(
            Manager manager,
            ManagerService.ProgressionResult progression) {

        if (progression.levelChanged()) {
            timelineService.recordEvent(
                    manager,
                    TimelineEventType.PROMOTION,
                    "Manager level increased",
                    "Reached level " + progression.currentLevel() + ".",
                    null,
                    null
            );
        }

        if (progression.reputationChanged()) {
            timelineService.recordEvent(
                    manager,
                    TimelineEventType.REPUTATION_UPGRADE,
                    "Reputation upgraded",
                    "Reputation upgraded to "
                            + progression.currentReputation() + ".",
                    null,
                    null
            );
        }
    }

    private CareerStatistics getOrCreateStatistics(Manager manager) {
        return careerStatisticsRepository.findByManagerId(manager.getId())
                .orElseGet(() -> createStatistics(manager));
    }

    private CareerStatistics createStatistics(Manager manager) {
        CareerStatistics statistics = CareerStatistics.builder()
                .manager(manager)
                .tournamentsManaged(0)
                .matchesManaged(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .goalsScored(0)
                .goalsConceded(0)
                .cleanSheets(0)
                .trophiesWon(0)
                .finalsReached(0)
                .semiFinalsReached(0)
                .updatedAt(LocalDateTime.now())
                .build();

        return careerStatisticsRepository.save(statistics);
    }

    private CareerStatisticsResponse mapToResponse(
            CareerStatistics statistics) {

        return new CareerStatisticsResponse(
                statistics.getManager().getId(),
                statistics.getTournamentsManaged(),
                statistics.getMatchesManaged(),
                statistics.getWins(),
                statistics.getDraws(),
                statistics.getLosses(),
                statistics.getGoalsScored(),
                statistics.getGoalsConceded(),
                statistics.getCleanSheets(),
                statistics.getTrophiesWon(),
                statistics.getFinalsReached(),
                statistics.getSemiFinalsReached()
        );
    }
}
