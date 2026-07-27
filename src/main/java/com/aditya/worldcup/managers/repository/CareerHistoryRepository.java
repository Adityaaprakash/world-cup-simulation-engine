package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.CareerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerHistoryRepository
        extends JpaRepository<CareerHistory, Long> {

    List<CareerHistory> findByManagerIdOrderByDateCompletedDesc(Long managerId);

    boolean existsByManagerIdAndTournamentIdAndTeamId(
            Long managerId,
            Long tournamentId,
            Long teamId
    );
}
