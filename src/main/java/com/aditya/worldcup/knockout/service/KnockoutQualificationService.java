package com.aditya.worldcup.knockout.service;

import com.aditya.worldcup.groups.entity.Group;
import com.aditya.worldcup.groups.repository.GroupRepository;
import com.aditya.worldcup.knockout.dto.KnockoutBracketResponse;
import com.aditya.worldcup.knockout.dto.KnockoutMatchResponse;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.shared.exception.FixturesNotGeneratedException;
import com.aditya.worldcup.shared.exception.GroupsNotGeneratedException;
import com.aditya.worldcup.shared.exception.KnockoutAlreadyGeneratedException;
import com.aditya.worldcup.shared.exception.TournamentNotFoundException;
import com.aditya.worldcup.standings.entity.Standing;
import com.aditya.worldcup.standings.repository.StandingRepository;
import com.aditya.worldcup.standings.service.StandingService;
import com.aditya.worldcup.teams.entity.Team;
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
public class KnockoutQualificationService {

    private final TournamentRepository tournamentRepository;
    private final GroupRepository groupRepository;
    private final StandingRepository standingRepository;
    private final StandingService standingService;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchRepository matchRepository;

    @Transactional
    public KnockoutBracketResponse generateKnockout(Long tournamentId) {

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (!matchRepository.existsByTournamentIdAndRound(
                tournamentId,
                MatchRound.GROUP_STAGE)) {
            throw new FixturesNotGeneratedException();
        }

        if (matchRepository.existsByTournamentIdAndRound(
                tournamentId,
                MatchRound.ROUND_OF_16)) {
            throw new KnockoutAlreadyGeneratedException();
        }

        List<Group> groups = groupRepository.findByTournamentId(tournamentId)
                .stream()
                .sorted(Comparator.comparing(Group::getName))
                .toList();

        if (groups.isEmpty()) {
            throw new GroupsNotGeneratedException();
        }

        List<GroupQualifier> qualifiers = groups.stream()
                .map(group -> getGroupQualifier(tournament, group))
                .toList();

        List<Match> knockoutMatches = buildKnockoutMatches(
                tournament,
                qualifiers
        );

        List<KnockoutMatchResponse> response = knockoutMatches.stream()
                .map(match -> new KnockoutMatchResponse(
                        match.getHomeTeam().getName(),
                        match.getAwayTeam().getName()
                ))
                .toList();

        return new KnockoutBracketResponse(
                MatchRound.ROUND_OF_16,
                response
        );
    }

    private GroupQualifier getGroupQualifier(
            Tournament tournament,
            Group group) {

        List<Standing> standings =
                getGroupStandings(tournament.getId(), group.getId());

        if (standings.size() < 2) {
            List<TournamentTeam> groupTeams =
                    tournamentTeamRepository.findByTournamentIdAndGroupId(
                            tournament.getId(),
                            group.getId()
                    );

            standingService.initializeStandings(
                    tournament,
                    group,
                    groupTeams
            );

            standings = getGroupStandings(
                    tournament.getId(),
                    group.getId()
            );
        }

        if (standings.size() < 2) {
            throw new GroupsNotGeneratedException();
        }

        return new GroupQualifier(
                standings.get(0).getTeam(),
                standings.get(1).getTeam()
        );
    }

    private List<Standing> getGroupStandings(
            Long tournamentId,
            Long groupId) {

        return standingRepository
                .findByTournamentIdAndGroupIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(
                        tournamentId,
                        groupId
                );
    }

    private List<Match> buildKnockoutMatches(
            Tournament tournament,
            List<GroupQualifier> qualifiers) {

        List<Match> matches = new ArrayList<>();
        LocalDateTime firstKnockoutDate = LocalDateTime.now().plusDays(30);

        for (int index = 0; index < qualifiers.size(); index += 2) {
            GroupQualifier current = qualifiers.get(index);
            GroupQualifier next = qualifiers.get((index + 1) % qualifiers.size());

            matches.add(saveKnockoutMatch(
                    tournament,
                    current.winner(),
                    next.runnerUp(),
                    firstKnockoutDate.plusDays(matches.size())
            ));

            matches.add(saveKnockoutMatch(
                    tournament,
                    next.winner(),
                    current.runnerUp(),
                    firstKnockoutDate.plusDays(matches.size())
            ));
        }

        return matches;
    }

    private Match saveKnockoutMatch(
            Tournament tournament,
            Team homeTeam,
            Team awayTeam,
            LocalDateTime matchDate) {

        Match match = Match.builder()
                .tournament(tournament)
                .group(null)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .matchDate(matchDate)
                .round(MatchRound.ROUND_OF_16)
                .status(MatchStatus.SCHEDULED)
                .build();

        return matchRepository.save(match);
    }

    private record GroupQualifier(
            Team winner,
            Team runnerUp
    ) {
    }
}
