package com.aditya.worldcup.tournamentteams.service;

import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.shared.exception.RegistrationClosedException;
import com.aditya.worldcup.shared.exception.TeamAlreadyRegisteredException;
import com.aditya.worldcup.shared.exception.TeamNotFoundException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournamentteams.dto.RegisterTeamRequest;
import com.aditya.worldcup.tournamentteams.dto.TournamentTeamResponse;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentTeamService {

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;

    public void registerTeam(
            Long tournamentId,
            RegisterTeamRequest request) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() ->
                        new TournamentNotFoundException(tournamentId));

        if (tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new RegistrationClosedException();
        }

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() ->
                        new TeamNotFoundException(request.teamId()));

        boolean alreadyRegistered =
                tournamentTeamRepository.existsByTournamentIdAndTeamId(
                        tournamentId,
                        team.getId()
                );

        if (alreadyRegistered) {
            throw new TeamAlreadyRegisteredException();
        }

        TournamentTeam tournamentTeam =
                TournamentTeam.builder()
                        .tournament(tournament)
                        .team(team)
                        .group(null)
                        .seed(0)
                        .build();

        tournamentTeamRepository.save(tournamentTeam);
    }

    public List<TournamentTeamResponse> getTournamentTeams(
            Long tournamentId) {

        return tournamentTeamRepository.findByTournamentId(tournamentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TournamentTeamResponse mapToResponse(
            TournamentTeam tournamentTeam) {

        Team team = tournamentTeam.getTeam();

        return new TournamentTeamResponse(
                tournamentTeam.getId(),
                team.getId(),
                team.getName(),
                team.getOverallRating(),
                tournamentTeam.getGroup() == null
                        ? null
                        : tournamentTeam.getGroup().getId(),
                tournamentTeam.getSeed()
        );
    }
}
