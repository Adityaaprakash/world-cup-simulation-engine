package com.aditya.worldcup.matchstatistics.repository;

import com.aditya.worldcup.matchstatistics.entity.MatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, Long> {
    Optional<MatchStatistics> findByMatchId(Long matchId);
}
