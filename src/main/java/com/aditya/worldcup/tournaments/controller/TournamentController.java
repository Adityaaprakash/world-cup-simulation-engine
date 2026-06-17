package com.aditya.worldcup.tournaments.controller;

import com.aditya.worldcup.tournaments.dto.CreateTournamentRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    public TournamentResponse createTournament(
            @Valid @RequestBody
            CreateTournamentRequest request) {

        return tournamentService.createTournament(request);
    }

    @GetMapping
    public List<TournamentResponse> getAllTournaments() {

        return tournamentService.getAllTournaments();
    }

    @GetMapping("/{id}")
    public TournamentResponse getTournament(
            @PathVariable Long id) {

        return tournamentService.getTournament(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTournament(
            @PathVariable Long id) {

        tournamentService.deleteTournament(id);
    }
}