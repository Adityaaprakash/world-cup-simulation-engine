package com.aditya.worldcup.squads.service;

import com.aditya.worldcup.formations.entity.Formation;
import com.aditya.worldcup.formations.repository.FormationRepository;
import com.aditya.worldcup.squads.dto.CreateSquadRequest;
import com.aditya.worldcup.squads.dto.SquadResponse;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import com.aditya.worldcup.users.entity.User;
import com.aditya.worldcup.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;
    private final TeamRepository teamRepository;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;

    public SquadResponse createSquad(
            CreateSquadRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() ->
                        new RuntimeException("Team not found"));

        Formation formation = formationRepository.findById(request.formationId())
                .orElseThrow(() ->
                        new RuntimeException("Formation not found"));

        Squad squad = Squad.builder()
                .user(user)
                .team(team)
                .formation(formation)
                .name(request.name())
                .createdAt(LocalDateTime.now())
                .build();

        squad = squadRepository.save(squad);

        return new SquadResponse(
                squad.getId(),
                squad.getName(),
                squad.getTeam().getName(),
                squad.getFormation().getName()
        );
    }

    public List<SquadResponse> getMySquads(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return squadRepository.findByUserId(user.getId())
                .stream()
                .map(squad -> new SquadResponse(
                        squad.getId(),
                        squad.getName(),
                        squad.getTeam().getName(),
                        squad.getFormation().getName()
                ))
                .toList();
    }
}