package com.aditya.worldcup.simulation.repository;

import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerMatchRatingRepository extends JpaRepository<PlayerMatchRating, Long> {
    List<PlayerMatchRating> findByMatchId(Long matchId);
    List<PlayerMatchRating> findByMatchIdIn(List<Long> matchIds);

    @Query("SELECT r.player, AVG(r.rating), COUNT(r) FROM PlayerMatchRating r WHERE r.player IS NOT NULL GROUP BY r.player ORDER BY AVG(r.rating) DESC")
    List<Object[]> findTopAverageRatedPlayers(Pageable pageable);
}
