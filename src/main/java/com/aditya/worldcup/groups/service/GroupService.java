package com.aditya.worldcup.groups.service;

import com.aditya.worldcup.groups.dto.GroupResponse;
import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.groups.repository.GroupRepository;
import com.aditya.worldcup.shared.exception.GroupsAlreadyGeneratedException;
import com.aditya.worldcup.shared.exception.NoRegisteredTeamsException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    public List<GroupResponse> generateGroups(Long tournamentId) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() ->
                        new TournamentNotFoundException(tournamentId));

        if (!groupRepository.findByTournamentId(tournamentId).isEmpty()) {
            throw new GroupsAlreadyGeneratedException();
        }

        List<TournamentTeam> tournamentTeams =
                tournamentTeamRepository.findByTournamentId(tournamentId);

        if (tournamentTeams.isEmpty()) {
            throw new NoRegisteredTeamsException();
        }

        Group[] groups = new Group[4];

        String[] names = {
                "Group A",
                "Group B",
                "Group C",
                "Group D"
        };

        for (int i = 0; i < names.length; i++) {

            groups[i] = groupRepository.save(
                    Group.builder()
                            .name(names[i])
                            .tournament(tournament)
                            .build()
            );
        }

        Collections.shuffle(tournamentTeams);

        for (int i = 0; i < tournamentTeams.size(); i++) {

            TournamentTeam tournamentTeam =
                    tournamentTeams.get(i);

            Group assignedGroup =
                    groups[i % groups.length];

            tournamentTeam.setGroup(assignedGroup);

            tournamentTeamRepository.save(tournamentTeam);
        }

        return getGroups(tournamentId);
    }

    public List<GroupResponse> getGroups(Long tournamentId) {

        return groupRepository.findByTournamentId(tournamentId)
                .stream()
                .map(group ->
                        new GroupResponse(
                                group.getId(),
                                group.getName()
                        ))
                .toList();
    }
}
