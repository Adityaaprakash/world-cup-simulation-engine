package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.shared.exception.FixturesNotGeneratedException;
import com.aditya.worldcup.shared.exception.GroupStageAlreadyCompletedException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.simulation.dto.GroupStageSimulationResponse;
import com.aditya.worldcup.standings.service.StandingService;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupStageSimulationService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TournamentMatchSimulationService tournamentMatchSimulationService;
    private final StandingService standingService;

    @Transactional
    public GroupStageSimulationResponse simulate(Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        List<Match> groupStageMatches =
                matchRepository.findByTournamentIdAndRoundOrderById(
                        tournamentId,
                        MatchRound.GROUP_STAGE
                );

        if (groupStageMatches.isEmpty()) {
            throw new FixturesNotGeneratedException();
        }

        List<Match> remainingMatches = groupStageMatches.stream()
                .filter(match -> match.getStatus() != MatchStatus.FINISHED)
                .toList();

        if (remainingMatches.isEmpty()) {
            throw new GroupStageAlreadyCompletedException();
        }

        remainingMatches.forEach(match ->
                tournamentMatchSimulationService.simulate(
                        tournamentId,
                        match.getId()
                )
        );

        int completedMatches = (int) groupStageMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .count();

        return new GroupStageSimulationResponse(
                tournamentId,
                remainingMatches.size(),
                completedMatches,
                standingService.getStandings(tournamentId)
        );
    }
}
