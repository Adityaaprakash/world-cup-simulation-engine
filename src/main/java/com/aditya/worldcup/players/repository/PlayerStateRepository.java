package com.aditya.worldcup.players.repository;

import com.aditya.worldcup.players.entity.PlayerState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerStateRepository extends JpaRepository<PlayerState, Long> {

    Optional<PlayerState> findByManagerIdAndPlayerId(Long managerId, Long playerId);
    
    List<PlayerState> findByManagerId(Long managerId);
    
    // Legacy method for tests where only one manager exists
    Optional<PlayerState> findByPlayerId(Long playerId);
}
