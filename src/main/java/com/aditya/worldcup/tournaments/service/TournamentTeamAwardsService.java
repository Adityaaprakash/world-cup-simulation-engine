package com.aditya.worldcup.tournaments.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import com.aditya.worldcup.matchstatistics.repository.MatchStatisticsRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.dto.TournamentTeamAwardsResponse;
import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import com.aditya.worldcup.tournamentteams.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TournamentTeamAwardsService {

    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;

    @Transactional(readOnly = true)
    public TournamentTeamAwardsResponse calculate(Long tournamentId) {
        Map<Long, TeamAwardStat> stats = new LinkedHashMap<>();
        tournamentTeamRepository.findByTournamentId(tournamentId)
                .stream()
                .map(TournamentTeam::getTeam)
                .forEach(team -> stats.put(team.getId(), new TeamAwardStat(team)));

        matchRepository.findByTournamentIdOrderById(tournamentId)
                .stream()
                .filter(match -> match.getStatus() == MatchStatus.FINISHED)
                .forEach(match -> collectMatch(match, stats));

        return new TournamentTeamAwardsResponse(
                buildAward(max(stats, Comparator.comparingInt(stat -> stat.goalsFor)),
                        TeamAwardStat::goalsFor),
                buildAward(min(stats, Comparator.comparingInt(stat -> stat.goalsAgainst)),
                        TeamAwardStat::goalsAgainst),
                buildAward(min(stats, Comparator.comparingInt(TeamAwardStat::disciplinePoints)),
                        TeamAwardStat::disciplinePoints)
        );
    }

    private void collectMatch(Match match, Map<Long, TeamAwardStat> stats) {
        TeamAwardStat home = stats.get(match.getHomeTeam().getId());
        TeamAwardStat away = stats.get(match.getAwayTeam().getId());
        if (home == null || away == null) {
            return;
        }
        home.goalsFor += match.getHomeScore();
        home.goalsAgainst += match.getAwayScore();
        away.goalsFor += match.getAwayScore();
        away.goalsAgainst += match.getHomeScore();

        matchStatisticsRepository.findByMatchId(match.getId())
                .ifPresent(statistics -> collectDiscipline(statistics, home, away));
    }

    private void collectDiscipline(MatchStatistics statistics,
                                   TeamAwardStat home,
                                   TeamAwardStat away) {
        home.yellowCards += statistics.getHomeYellowCards();
        home.redCards += statistics.getHomeRedCards();
        home.fouls += statistics.getHomeFouls();
        away.yellowCards += statistics.getAwayYellowCards();
        away.redCards += statistics.getAwayRedCards();
        away.fouls += statistics.getAwayFouls();
    }

    private TeamAwardStat max(Map<Long, TeamAwardStat> stats,
                              Comparator<TeamAwardStat> comparator) {
        return stats.values().stream().max(comparator).orElse(null);
    }

    private TeamAwardStat min(Map<Long, TeamAwardStat> stats,
                              Comparator<TeamAwardStat> comparator) {
        return stats.values().stream().min(comparator).orElse(null);
    }

    private TournamentTeamAwardsResponse.TeamAward buildAward(
            TeamAwardStat stat,
            java.util.function.ToIntFunction<TeamAwardStat> value) {
        if (stat == null) {
            return null;
        }
        return new TournamentTeamAwardsResponse.TeamAward(
                stat.team.getId(),
                stat.team.getName(),
                value.applyAsInt(stat)
        );
    }

    private static class TeamAwardStat {

        private final Team team;
        private int goalsFor;
        private int goalsAgainst;
        private int yellowCards;
        private int redCards;
        private int fouls;

        private TeamAwardStat(Team team) {
            this.team = team;
        }

        private int goalsFor() {
            return goalsFor;
        }

        private int goalsAgainst() {
            return goalsAgainst;
        }

        private int disciplinePoints() {
            return yellowCards + redCards * 3 + fouls / 4;
        }
    }
}
