package com.aditya.worldcup.simulation.repository;

import com.aditya.worldcup.simulation.entity.PlayerMatchRating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerMatchRatingRepository extends JpaRepository<PlayerMatchRating, Long> {
    List<PlayerMatchRating> findByMatchId(Long matchId);
}
