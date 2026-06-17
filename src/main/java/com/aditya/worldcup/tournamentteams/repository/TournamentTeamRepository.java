package com.aditya.worldcup.tournamentteams.repository;

import com.aditya.worldcup.tournamentteams.entity.TournamentTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentTeamRepository
        extends JpaRepository<TournamentTeam, Long> {

    List<TournamentTeam> findByTournamentId(Long tournamentId);

    boolean existsByTournamentIdAndTeamId(
            Long tournamentId,
            Long teamId
    );
}