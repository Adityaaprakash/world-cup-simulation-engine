package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matchevents.repository.MatchEventRepository;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.simulation.repository.PlayerMatchRatingRepository;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.standings.entity.Standing;
import com.aditya.worldcup.standings.repository.StandingRepository;
import com.aditya.worldcup.tournaments.dto.CreateTournamentRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final PlayerMatchRatingRepository playerMatchRatingRepository;
    private final StandingRepository standingRepository;

    @Transactional
    public TournamentResponse createTournament(
            CreateTournamentRequest request) {

        if (tournamentRepository.existsByNameIgnoreCaseAndYear(
                request.name(),
                request.year())) {
            throw new IllegalArgumentException(
                    "Tournament already exists for name and year");
        }

        Tournament tournament = Tournament.builder()
                .name(request.name())
                .year(request.year())
                .hostCountry(request.hostCountry())
                .status(TournamentStatus.UPCOMING)
                .createdAt(LocalDateTime.now())
                .build();

        tournament = tournamentRepository.save(tournament);

        return mapToResponse(tournament);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getAllTournaments() {

        return tournamentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> getTournamentPage(Pageable pageable) {

        return tournamentRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournament(Long id) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() ->
                        new TournamentNotFoundException(id));

        return mapToResponse(tournament);
    }

    @Transactional
    public void deleteTournament(Long id) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Tournament in progress cannot be deleted");
        }

        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed tournament cannot be deleted");
        }

        tournamentRepository.deleteById(id);
    }

    @Transactional
    public TournamentResponse archiveTournament(Long id) {
        Tournament tournament = findTournament(id);

        if (tournament.getStatus() != TournamentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only completed tournaments may be archived");
        }

        tournament.setStatus(TournamentStatus.ARCHIVED);
        return mapToResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse reopenArchivedTournament(Long id) {
        Tournament tournament = findTournament(id);

        if (tournament.getStatus() != TournamentStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Only archived tournaments may be reopened");
        }

        tournament.setStatus(TournamentStatus.COMPLETED);
        return mapToResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public TournamentResponse resetTournament(Long id) {
        Tournament tournament = findTournament(id);

        if (tournament.getStatus() == TournamentStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Archived tournaments must be reopened before reset");
        }

        List<Match> matches = matchRepository.findByTournamentIdOrderById(id);
        List<Long> matchIds = matches.stream()
                .map(Match::getId)
                .toList();

        if (!matchIds.isEmpty()) {
            matchEventRepository.deleteByMatchIdIn(matchIds);
            matchStatisticsRepository.deleteByMatchIdIn(matchIds);
            playerMatchRatingRepository.deleteByMatchIdIn(matchIds);
        }

        matches.forEach(match -> {
            match.setHomeScore(null);
            match.setAwayScore(null);
            match.setStatus(MatchStatus.SCHEDULED);
            match.setManOfTheMatch(null);
        });
        matchRepository.saveAll(matches);

        List<Standing> standings = standingRepository
                .findByTournamentIdOrderByGroupNameAscPointsDescGoalDifferenceDescGoalsForDesc(id);
        standings.forEach(this::resetStanding);
        standingRepository.saveAll(standings);

        tournament.setStatus(TournamentStatus.UPCOMING);
        return mapToResponse(tournamentRepository.save(tournament));
    }

    @Transactional
    public void deleteInactiveTournament(Long id) {
        Tournament tournament = findTournament(id);

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Active tournaments cannot be deleted");
        }

        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed tournaments must be archived before deletion");
        }

        tournamentRepository.delete(tournament);
    }

    public TournamentResponse mapToResponse(
            Tournament tournament) {

        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getYear(),
                tournament.getHostCountry(),
                tournament.getStatus(),
                tournament.getCreatedAt()
        );
    }

    private Tournament findTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
    }

    private void resetStanding(Standing standing) {
        standing.setPlayed(0);
        standing.setWon(0);
        standing.setDrawn(0);
        standing.setLost(0);
        standing.setGoalsFor(0);
        standing.setGoalsAgainst(0);
        standing.setGoalDifference(0);
        standing.setPoints(0);
    }
}
