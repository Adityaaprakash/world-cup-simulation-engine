package com.aditya.worldcup.teams.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.teams.dto.TeamResponse;
import com.aditya.worldcup.teams.service.TeamService;
import com.aditya.worldcup.matches.dto.MatchHistoryResponse;
import com.aditya.worldcup.matches.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final MatchService matchService;

    @GetMapping
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams();
    }

    @GetMapping("/{id}")
    public TeamResponse getTeam(
            @PathVariable Long id
    ) {
        return teamService.getTeam(id);
    }

    @GetMapping("/{id}/players")
    public List<PlayerResponse> getTeamPlayers(
            @PathVariable Long id
    ) {
        return teamService.getTeamPlayers(id);
    }

    @GetMapping("/{id}/matches")
    public List<MatchHistoryResponse> getTeamMatches(
            @PathVariable Long id
    ) {
        return matchService.getTeamMatchHistory(id);
    }
}