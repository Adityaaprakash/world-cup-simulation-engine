package com.aditya.worldcup.squadplayers.repository;

import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SquadPlayerRepository extends JpaRepository<SquadPlayer, Long> {

    List<SquadPlayer> findBySquadId(Long squadId);

    boolean existsBySquadIdAndPlayerId(Long squadId, Long playerId);

    long countBySquadId(Long squadId);

}