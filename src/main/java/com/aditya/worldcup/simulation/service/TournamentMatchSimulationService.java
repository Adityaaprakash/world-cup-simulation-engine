package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.simulation.dto.MatchSimulationRequest;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import com.aditya.worldcup.simulation.dto.TournamentMatchSimulationResponse;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.standings.service.StandingUpdateService;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentMatchSimulationService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final SquadRepository squadRepository;
    private final MatchSimulationService matchSimulationService;
    private final StandingUpdateService standingUpdateService;

    @Transactional
    public TournamentMatchSimulationResponse simulate(
            Long tournamentId,
            Long matchId
    ) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Match not found with id: " + matchId
                ));

        if (match.getTournament() == null
                || !match.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException(
                    "Match does not belong to tournament: " + tournamentId
            );
        }

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalArgumentException(
                    "Match has already been completed"
            );
        }

        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new IllegalArgumentException(
                    "Match must have both home and away teams"
            );
        }

        Squad homeSquad = squadRepository
                .findFirstByTeamId(
                        match.getHomeTeam().getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No squad found for home team: "
                                        + match.getHomeTeam().getId()
                        ));

        Squad awaySquad = squadRepository
                .findFirstByTeamId(
                        match.getAwayTeam().getId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No squad found for away team: "
                                        + match.getAwayTeam().getId()
                        ));

        MatchSimulationResponse simulation =
                matchSimulationService.simulate(
                        new MatchSimulationRequest(
                                homeSquad.getId(),
                                awaySquad.getId()
                        )
                );

        match.setHomeScore(
                simulation.homeGoals()
        );

        match.setAwayScore(
                simulation.awayGoals()
        );

        match.setStatus(
                MatchStatus.FINISHED
        );

        matchRepository.save(match);

        if (match.getGroup() != null) {
            standingUpdateService.updateStandings(
                    match,
                    simulation.homeGoals(),
                    simulation.awayGoals()
            );
        }

        return new TournamentMatchSimulationResponse(
                match.getId(),
                simulation.homeTeam(),
                simulation.awayTeam(),
                simulation.homeGoals(),
                simulation.awayGoals(),
                simulation.winner(),
                match.getStatus().name(),
                simulation.events(),
                simulation.statistics(),
                simulation.playerRatings(),
                simulation.manOfTheMatch(),
                simulation.commentary()
        );
    }
}
