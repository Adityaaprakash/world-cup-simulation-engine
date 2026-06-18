package com.aditya.worldcup.fixtures.service;

import com.aditya.worldcup.fixtures.dto.FixtureGenerationResponse;
import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.groups.repository.GroupRepository;
import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matches.service.MatchService;
import com.aditya.worldcup.shared.exception.FixturesAlreadyGeneratedException;
import com.aditya.worldcup.shared.exception.GroupsNotGeneratedException;
import com.aditya.worldcup.shared.exception.NoRegisteredTeamsException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.standings.service.StandingService;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FixtureGenerationService {

    private final TournamentRepository tournamentRepository;
    private final GroupRepository groupRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final StandingService standingService;

    @Transactional
    public FixtureGenerationResponse generateFixtures(Long tournamentId) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (matchRepository.existsByTournamentIdAndRound(
                tournamentId,
                MatchRound.GROUP_STAGE)) {
            throw new FixturesAlreadyGeneratedException();
        }

        List<Group> groups = groupRepository.findByTournamentId(tournamentId)
                .stream()
                .sorted(Comparator.comparing(Group::getName))
                .toList();

        if (groups.isEmpty()) {
            throw new GroupsNotGeneratedException();
        }

        if (tournamentTeamRepository.findByTournamentId(tournamentId).isEmpty()) {
            throw new NoRegisteredTeamsException();
        }

        List<Match> generatedMatches = new ArrayList<>();
        LocalDateTime firstMatchDate = LocalDateTime.now().plusDays(1);
        int matchOffset = 0;

        for (Group group : groups) {
            List<TournamentTeam> groupTeams =
                    tournamentTeamRepository.findByTournamentIdAndGroupId(
                                    tournamentId,
                                    group.getId()
                            )
                            .stream()
                            .sorted(Comparator.comparing(tt -> tt.getTeam().getName()))
                            .toList();

            if (groupTeams.isEmpty()) {
                throw new GroupsNotGeneratedException();
            }

            standingService.initializeStandings(
                    tournament,
                    group,
                    groupTeams
            );

            for (int homeIndex = 0; homeIndex < groupTeams.size(); homeIndex++) {
                for (int awayIndex = homeIndex + 1; awayIndex < groupTeams.size(); awayIndex++) {
                    Match match = Match.builder()
                            .tournament(tournament)
                            .group(group)
                            .homeTeam(groupTeams.get(homeIndex).getTeam())
                            .awayTeam(groupTeams.get(awayIndex).getTeam())
                            .matchDate(firstMatchDate.plusDays(matchOffset++))
                            .round(MatchRound.GROUP_STAGE)
                            .status(MatchStatus.SCHEDULED)
                            .build();

                    generatedMatches.add(matchRepository.save(match));
                }
            }
        }

        List<MatchResponse> response = generatedMatches.stream()
                .map(matchService::mapToResponse)
                .toList();

        return new FixtureGenerationResponse(response);
    }
}
