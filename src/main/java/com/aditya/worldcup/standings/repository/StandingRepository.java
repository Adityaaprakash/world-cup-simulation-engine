package com.aditya.worldcup.standings.repository;

import com.aditya.worldcup.standings.entity.Standing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository
        extends JpaRepository<Standing, Long> {

    List<Standing> findByTournamentIdOrderByGroupNameAscPointsDescGoalDifferenceDescGoalsForDesc(
            Long tournamentId
    );

    List<Standing> findByTournamentIdAndGroupIdOrderByPointsDescGoalDifferenceDescGoalsForDesc(
            Long tournamentId,
            Long groupId
    );

    boolean existsByTournamentIdAndGroupIdAndTeamId(
            Long tournamentId,
            Long groupId,
            Long teamId
    );

    Optional<Standing> findByTournamentIdAndGroupIdAndTeamId(
            Long tournamentId,
            Long groupId,
            Long teamId
    );
}
