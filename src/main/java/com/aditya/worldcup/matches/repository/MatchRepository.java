package com.aditya.worldcup.matches.repository;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository
        extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderById(Long tournamentId);

    List<Match> findByTournamentIdAndRoundOrderById(
            Long tournamentId,
            MatchRound round
    );

    boolean existsByTournamentIdAndRound(
            Long tournamentId,
            MatchRound round
    );

    @Query("SELECT m FROM Match m WHERE m.status = com.aditya.worldcup.matches.entity.MatchStatus.FINISHED ORDER BY m.matchDate DESC, m.id DESC")
    List<Match> findCompletedMatchesHistory();

    @Query("SELECT m FROM Match m WHERE m.status = com.aditya.worldcup.matches.entity.MatchStatus.FINISHED AND (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) ORDER BY m.matchDate DESC, m.id DESC")
    List<Match> findCompletedMatchesByTeamId(@Param("teamId") Long teamId);
}
