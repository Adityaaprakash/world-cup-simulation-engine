package com.aditya.worldcup.matches.repository;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
