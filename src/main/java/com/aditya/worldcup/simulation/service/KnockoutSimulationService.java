package com.aditya.worldcup.simulation.service;

import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matches.service.MatchService;
import com.aditya.worldcup.shared.exception.FixturesNotGeneratedException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.simulation.dto.KnockoutSimulationResponse;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class KnockoutSimulationService {

    private static final List<MatchRound> KNOCKOUT_ROUNDS = List.of(
            MatchRound.ROUND_OF_16,
            MatchRound.QUARTER_FINALS,
            MatchRound.SEMI_FINALS,
            MatchRound.FINAL
    );

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TournamentMatchSimulationService
            tournamentMatchSimulationService;
    private final MatchService matchService;
    private final Random random = new Random();

    @Transactional
    public KnockoutSimulationResponse simulate(Long tournamentId) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(
                        tournamentId
                ));

        if (!matchRepository.existsByTournamentIdAndRound(
                tournamentId,
                MatchRound.ROUND_OF_16
        )) {
            throw new FixturesNotGeneratedException();
        }

        List<MatchResponse> simulatedMatches = new ArrayList<>();
        Team champion = null;

        for (int roundIndex = 0;
             roundIndex < KNOCKOUT_ROUNDS.size();
             roundIndex++) {

            MatchRound round = KNOCKOUT_ROUNDS.get(roundIndex);
            List<Match> matches = getRoundMatches(tournamentId, round);

            if (matches.isEmpty() && roundIndex > 0) {
                createNextRoundFixtures(
                        tournament,
                        KNOCKOUT_ROUNDS.get(roundIndex - 1),
                        round
                );
                matches = getRoundMatches(tournamentId, round);
            }

            validateRoundFixtures(round, matches);

            for (Match match : matches) {
                validateMatchTeams(match);

                if (match.getStatus() != MatchStatus.FINISHED) {
                    tournamentMatchSimulationService.simulate(
                            tournamentId,
                            match.getId()
                    );

                    Match simulatedMatch = matchRepository
                            .findById(match.getId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Match not found with id: "
                                            + match.getId()
                            ));

                    resolveDrawIfNeeded(simulatedMatch);
                    simulatedMatches.add(
                            matchService.mapToResponse(simulatedMatch)
                    );
                }
            }

            if (round == MatchRound.FINAL) {
                Match finalMatch = matches.get(0);
                champion = determineWinner(finalMatch);
            }
        }

        return new KnockoutSimulationResponse(
                tournamentId,
                champion.getName(),
                simulatedMatches,
                countCompletedKnockoutMatches(tournamentId)
        );
    }

    private List<Match> getRoundMatches(
            Long tournamentId,
            MatchRound round
    ) {

        return matchRepository.findByTournamentIdAndRoundOrderById(
                tournamentId,
                round
        );
    }

    private void validateRoundFixtures(
            MatchRound round,
            List<Match> matches
    ) {

        if (matches.size() != expectedMatchCount(round)) {
            throw new IllegalArgumentException(
                    "Missing fixtures for knockout round: " + round
            );
        }
    }

    private int expectedMatchCount(MatchRound round) {

        return switch (round) {
            case ROUND_OF_16 -> 8;
            case QUARTER_FINALS -> 4;
            case SEMI_FINALS -> 2;
            case FINAL -> 1;
            default -> throw new IllegalArgumentException(
                    "Unsupported knockout round: " + round
            );
        };
    }

    private void validateMatchTeams(Match match) {

        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new IllegalArgumentException(
                    "Match must have both home and away teams"
            );
        }
    }

    private void createNextRoundFixtures(
            Tournament tournament,
            MatchRound completedRound,
            MatchRound nextRound
    ) {

        List<Match> completedMatches = getRoundMatches(
                tournament.getId(),
                completedRound
        );

        validateRoundFixtures(completedRound, completedMatches);

        List<Team> winners = completedMatches.stream()
                .map(this::determineWinner)
                .toList();

        if (winners.size() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Cannot create fixtures from an odd number of winners"
            );
        }

        LocalDateTime nextMatchDate = completedMatches.stream()
                .map(Match::getMatchDate)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now())
                .plusDays(3);

        for (int index = 0; index < winners.size(); index += 2) {
            Match match = Match.builder()
                    .tournament(tournament)
                    .group(null)
                    .homeTeam(winners.get(index))
                    .awayTeam(winners.get(index + 1))
                    .matchDate(nextMatchDate.plusDays(index / 2))
                    .round(nextRound)
                    .status(MatchStatus.SCHEDULED)
                    .build();

            matchRepository.save(match);
        }
    }

    private Team determineWinner(Match match) {

        if (match.getStatus() != MatchStatus.FINISHED) {
            throw new IllegalArgumentException(
                    "Match has not been completed: " + match.getId()
            );
        }

        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            throw new IllegalArgumentException(
                    "Completed match is missing scores: " + match.getId()
            );
        }

        if (match.getHomeScore() > match.getAwayScore()) {
            return match.getHomeTeam();
        }

        if (match.getAwayScore() > match.getHomeScore()) {
            return match.getAwayTeam();
        }

        throw new IllegalArgumentException(
                "Completed knockout match cannot end in a draw: "
                        + match.getId()
        );
    }

    private void resolveDrawIfNeeded(Match match) {

        if (!match.getHomeScore().equals(match.getAwayScore())) {
            return;
        }

        if (random.nextBoolean()) {
            match.setHomeScore(match.getHomeScore() + 1);
        } else {
            match.setAwayScore(match.getAwayScore() + 1);
        }

        matchRepository.save(match);
    }

    private int countCompletedKnockoutMatches(Long tournamentId) {

        return (int) KNOCKOUT_ROUNDS.stream()
                .flatMap(round -> getRoundMatches(tournamentId, round)
                        .stream())
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .count();
    }
}
