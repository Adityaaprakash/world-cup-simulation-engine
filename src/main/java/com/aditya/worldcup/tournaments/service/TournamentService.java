package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.tournaments.dto.CreateTournamentRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentResponse createTournament(
            CreateTournamentRequest request) {

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

    public TournamentResponse getTournament(Long id) {

        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() ->
                        new TournamentNotFoundException(id));

        return mapToResponse(tournament);
    }

    public void deleteTournament(Long id) {

        if (!tournamentRepository.existsById(id)) {
            throw new TournamentNotFoundException(id);
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
