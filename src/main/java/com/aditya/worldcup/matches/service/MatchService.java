package com.aditya.worldcup.matches.service;

import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.dto.MatchResultRequest;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.standings.service.StandingUpdateService;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final StandingUpdateService standingUpdateService;

    public List<MatchResponse> getTournamentMatches(Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return matchRepository.findByTournamentIdOrderById(tournamentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MatchResponse completeMatch(
            Long tournamentId,
            Long matchId,
            MatchResultRequest request
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

        validateResult(request);

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalArgumentException(
                    "Match has already been completed"
            );
        }

        match.setHomeScore(request.homeGoals());
        match.setAwayScore(request.awayGoals());
        match.setStatus(MatchStatus.FINISHED);

        Match savedMatch = matchRepository.save(match);

        standingUpdateService.updateStandings(
                savedMatch,
                request.homeGoals(),
                request.awayGoals()
        );

        return mapToResponse(savedMatch);
    }

    public MatchResponse mapToResponse(Match match) {

        return new MatchResponse(
                match.getId(),
                match.getGroup() == null
                        ? null
                        : match.getGroup().getName(),
                match.getRound(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getStatus()
        );
    }

    private void validateResult(MatchResultRequest request) {

        if (request.homeGoals() == null || request.awayGoals() == null) {
            throw new IllegalArgumentException(
                    "Both homeGoals and awayGoals are required"
            );
        }

        if (request.homeGoals() < 0 || request.awayGoals() < 0) {
            throw new IllegalArgumentException(
                    "Goals cannot be negative"
            );
        }
    }
}
