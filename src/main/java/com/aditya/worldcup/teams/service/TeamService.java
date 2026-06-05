package com.aditya.worldcup.teams.service;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.teams.dto.TeamResponse;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public List<TeamResponse> getAllTeams() {

        return teamRepository.findAll()
                .stream()
                .map(team -> new TeamResponse(
                        team.getId(),
                        team.getName(),
                        team.getOverallRating()
                ))
                .toList();
    }

    public TeamResponse getTeam(Long id) {

        Team team = teamRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getOverallRating()
        );
    }

    public List<PlayerResponse> getTeamPlayers(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));

        List<Player> players =
                playerRepository.findByCountryId(
                        team.getCountry().getId()
                );

        return players.stream()
                .map(player -> new PlayerResponse(
                        player.getId(),
                        player.getName(),
                        player.getPosition().name(),
                        player.getOverallRating()
                ))
                .toList();
    }
}