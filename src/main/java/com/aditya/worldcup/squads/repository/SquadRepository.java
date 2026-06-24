package com.aditya.worldcup.squads.repository;

import com.aditya.worldcup.squads.entity.Squad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByUserId(Long userId);

    Optional<Squad> findFirstByTeamId(Long teamId);
}