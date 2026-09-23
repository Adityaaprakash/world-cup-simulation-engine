package com.aditya.worldcup.contracts.repository;

import com.aditya.worldcup.contracts.entity.PlayerLifecycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerLifecycleRepository extends JpaRepository<PlayerLifecycle, Long> {
    Optional<PlayerLifecycle> findByManagerIdAndPlayerId(Long managerId, Long playerId);
}
