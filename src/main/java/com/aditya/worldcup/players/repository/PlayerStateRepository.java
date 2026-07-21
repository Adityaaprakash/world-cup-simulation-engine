package com.aditya.worldcup.players.repository;

import com.aditya.worldcup.players.entity.PlayerState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerStateRepository extends JpaRepository<PlayerState, Long> {

    Optional<PlayerState> findByPlayerId(Long playerId);
}
