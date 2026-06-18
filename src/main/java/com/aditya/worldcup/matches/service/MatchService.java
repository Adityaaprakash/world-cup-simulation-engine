package com.aditya.worldcup.matches.service;

import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;

    public List<MatchResponse> getTournamentMatches(Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        return matchRepository.findByTournamentIdOrderById(tournamentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
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
                match.getStatus()
        );
    }
}
