package com.aditya.worldcup.tactics.repository;

import com.aditya.worldcup.tactics.entity.MatchPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MatchPlanRepository extends JpaRepository<MatchPlan, Long> {
    Optional<MatchPlan> findByMatchIdAndSquadIdAndManagerId(Long matchId, Long squadId, Long managerId);
    List<MatchPlan> findByManagerId(Long managerId);
}
