package com.aditya.worldcup.tactics.repository;

import com.aditya.worldcup.tactics.entity.TacticalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TacticalProfileRepository extends JpaRepository<TacticalProfile, Long> {

    Optional<TacticalProfile> findByTeamId(Long teamId);
}
