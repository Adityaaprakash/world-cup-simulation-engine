package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.tournaments.dto.CreateTournamentRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;

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

    public List<TournamentResponse> getAllTournaments() {

        return tournamentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Page<TournamentResponse> getTournamentPage(Pageable pageable) {

        return tournamentRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public TournamentResponse getTournament(Long id) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() ->
                        new TournamentNotFoundException(id));

        return mapToResponse(tournament);
    }

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

    private TournamentResponse mapToResponse(
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
}
