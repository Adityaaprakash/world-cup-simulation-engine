package com.aditya.worldcup.standings.service;

import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.groups.repository.GroupRepository;
import com.aditya.worldcup.shared.exception.GroupsNotGeneratedException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.standings.dto.GroupStandingsResponse;
import com.aditya.worldcup.standings.dto.StandingResponse;
import com.aditya.worldcup.standings.entity.Standing;
import com.aditya.worldcup.standings.repository.StandingRepository;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StandingService {

    private final StandingRepository standingRepository;
    private final TournamentRepository tournamentRepository;
    private final GroupRepository groupRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    public List<GroupStandingsResponse> getStandings(Long tournamentId) {

        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }

        List<Group> groups = groupRepository.findByTournamentId(tournamentId);

        if (groups.isEmpty()) {
            throw new GroupsNotGeneratedException();
        }

        return groups.stream()
                .sorted(Comparator.comparing(Group::getName))
                .map(group -> new GroupStandingsResponse(
                        group.getName(),
                        standingRepository
                                .findByTournamentIdAndGroupIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(
                                        tournamentId,
                                        group.getId()
                                )
                                .stream()
                                .map(this::mapToResponse)
                                .toList()
                ))
                .toList();
    }

    public void initializeStandings(
            Tournament tournament,
            Group group,
            List<TournamentTeam> tournamentTeams) {

        for (TournamentTeam tournamentTeam : tournamentTeams) {
            boolean exists = standingRepository.existsByTournamentIdAndGroupIdAndTeamId(
                    tournament.getId(),
                    group.getId(),
                    tournamentTeam.getTeam().getId()
            );

            if (!exists) {
                standingRepository.save(
                        Standing.builder()
                                .tournament(tournament)
                                .group(group)
                                .team(tournamentTeam.getTeam())
                                .played(0)
                                .won(0)
                                .drawn(0)
                                .lost(0)
                                .goalsFor(0)
                                .goalsAgainst(0)
                                .goalDifference(0)
                                .points(0)
                                .build()
                );
            }
        }
    }

    private StandingResponse mapToResponse(Standing standing) {

        return new StandingResponse(
                standing.getTeam().getName(),
                standing.getPoints(),
                standing.getWon(),
                standing.getDrawn(),
                standing.getLost(),
                standing.getGoalsFor(),
                standing.getGoalsAgainst(),
                standing.getGoalDifference()
        );
    }
}
