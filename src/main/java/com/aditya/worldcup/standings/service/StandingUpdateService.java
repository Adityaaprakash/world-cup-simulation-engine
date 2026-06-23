package com.aditya.worldcup.standings.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.standings.entity.Standing;
import com.aditya.worldcup.standings.repository.StandingRepository;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StandingUpdateService {

    private final StandingRepository standingRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    @Transactional
    public void updateStandings(
            Match match,
            int homeGoals,
            int awayGoals
    ) {

        validateMatch(match);

        Long tournamentId = match.getTournament().getId();
        Long groupId = match.getGroup().getId();
        Long homeTeamId = match.getHomeTeam().getId();
        Long awayTeamId = match.getAwayTeam().getId();

        validateTournamentTeam(tournamentId, homeTeamId);
        validateTournamentTeam(tournamentId, awayTeamId);

        Standing homeStanding = findStanding(
                tournamentId,
                groupId,
                homeTeamId
        );

        Standing awayStanding = findStanding(
                tournamentId,
                groupId,
                awayTeamId
        );

        applyResult(
                homeStanding,
                homeGoals,
                awayGoals
        );

        applyResult(
                awayStanding,
                awayGoals,
                homeGoals
        );

        standingRepository.save(homeStanding);
        standingRepository.save(awayStanding);
    }

    private void validateMatch(Match match) {

        if (match.getTournament() == null) {
            throw new IllegalArgumentException(
                    "Match is not assigned to a tournament"
            );
        }

        if (match.getGroup() == null) {
            throw new IllegalArgumentException(
                    "Match is not assigned to a group"
            );
        }

        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new IllegalArgumentException(
                    "Match must have both home and away teams"
            );
        }
    }

    private void validateTournamentTeam(
            Long tournamentId,
            Long teamId
    ) {

        if (!tournamentTeamRepository.existsByTournamentIdAndTeamId(
                tournamentId,
                teamId
        )) {
            throw new IllegalArgumentException(
                    "Team is not registered in tournament: " + teamId
            );
        }
    }

    private Standing findStanding(
            Long tournamentId,
            Long groupId,
            Long teamId
    ) {

        return standingRepository
                .findByTournamentIdAndGroupIdAndTeamId(
                        tournamentId,
                        groupId,
                        teamId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Standing not found for team: " + teamId
                ));
    }

    private void applyResult(
            Standing standing,
            int goalsFor,
            int goalsAgainst
    ) {

        standing.setPlayed(standing.getPlayed() + 1);
        standing.setGoalsFor(
                standing.getGoalsFor() + goalsFor
        );
        standing.setGoalsAgainst(
                standing.getGoalsAgainst() + goalsAgainst
        );
        standing.setGoalDifference(
                standing.getGoalsFor() - standing.getGoalsAgainst()
        );

        if (goalsFor > goalsAgainst) {
            standing.setWon(standing.getWon() + 1);
            standing.setPoints(standing.getPoints() + 3);
        } else if (goalsFor < goalsAgainst) {
            standing.setLost(standing.getLost() + 1);
        } else {
            standing.setDrawn(standing.getDrawn() + 1);
            standing.setPoints(standing.getPoints() + 1);
        }
    }
}
