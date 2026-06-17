package com.aditya.worldcup.tournamentteams.controller;

import com.aditya.worldcup.tournamentteams.dto.RegisterTeamRequest;
import com.aditya.worldcup.tournamentteams.dto.TournamentTeamResponse;
import com.aditya.worldcup.tournamentteams.service.TournamentTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentTeamController {

    private final TournamentTeamService tournamentTeamService;

    @PostMapping("/{id}/register")
    public void registerTeam(
            @PathVariable Long id,
            @Valid @RequestBody RegisterTeamRequest request) {

        tournamentTeamService.registerTeam(id, request);
    }

    @GetMapping("/{id}/teams")
    public List<TournamentTeamResponse> getTournamentTeams(
            @PathVariable Long id) {

        return tournamentTeamService.getTournamentTeams(id);
    }
}
