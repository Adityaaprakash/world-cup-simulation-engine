package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.ChanceCreation;
import com.aditya.worldcup.tactics.entity.MatchPlan;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.repository.MatchPlanRepository;
import com.aditya.worldcup.tactics.dto.MatchPlanDto;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.squads.repository.SquadRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchPlanService {

    private final MatchPlanRepository matchPlanRepository;
    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;

    public Optional<MatchPlan> getMatchPlan(Long matchId, Long squadId, Long managerId) {
        if (managerId == null) {
            return Optional.empty();
        }
        return matchPlanRepository.findByMatchIdAndSquadIdAndManagerId(matchId, squadId, managerId);
    }
    
    @Transactional
    public MatchPlan saveMatchPlan(Long matchId, Long managerId, MatchPlanDto request) {
        MatchPlan plan = matchPlanRepository.findByMatchIdAndSquadIdAndManagerId(matchId, request.squadId(), managerId)
                .orElse(MatchPlan.builder()
                        .match(matchRepository.findById(matchId).orElseThrow())
                        .squad(squadRepository.findById(request.squadId()).orElseThrow())
                        .managerId(managerId)
                        .build());
                        
        plan.setTacticalApproach(request.tacticalApproach());
        plan.setPressingIntensity(request.pressingIntensity());
        plan.setTempo(request.tempo());
        plan.setDefensiveLine(request.defensiveLine());
        plan.setAttackingWidth(request.attackingWidth());
        plan.setCounterAttack(request.counterAttack());
        plan.setOffsideTrap(request.offsideTrap());
        
        return matchPlanRepository.save(plan);
    }
    
    public MatchPlanDto mapToDto(MatchPlan plan) {
        if (plan == null) return null;
        return MatchPlanDto.builder()
                .matchId(plan.getMatch().getId())
                .squadId(plan.getSquad().getId())
                .tacticalApproach(plan.getTacticalApproach())
                .pressingIntensity(plan.getPressingIntensity())
                .tempo(plan.getTempo())
                .defensiveLine(plan.getDefensiveLine())
                .attackingWidth(plan.getAttackingWidth())
                .counterAttack(plan.getCounterAttack())
                .offsideTrap(plan.getOffsideTrap())
                .build();
    }

    public TacticalProfile applyMatchPlanOverrides(TacticalProfile baseProfile, MatchPlan plan) {
        if (plan == null) {
            return baseProfile;
        }
        
        TacticalProfile modified = new TacticalProfile();
        modified.setId(baseProfile.getId());
        modified.setTeam(baseProfile.getTeam());
        
        // Copy base stats
        modified.setAttackWidth(baseProfile.getAttackWidth());
        modified.setDefensiveWidth(baseProfile.getDefensiveWidth());
        modified.setDefensiveLine(baseProfile.getDefensiveLine());
        modified.setPressingIntensity(baseProfile.getPressingIntensity());
        modified.setBuildUpStyle(baseProfile.getBuildUpStyle());
        modified.setChanceCreation(baseProfile.getChanceCreation());
        modified.setAttackingWidth(baseProfile.getAttackingWidth());
        modified.setCrossFrequency(baseProfile.getCrossFrequency());
        modified.setLongBallFrequency(baseProfile.getLongBallFrequency());
        modified.setPassingRisk(baseProfile.getPassingRisk());
        modified.setCounterAttack(baseProfile.getCounterAttack());
        modified.setHighPress(baseProfile.getHighPress());
        modified.setOffsideTrap(baseProfile.getOffsideTrap());
        modified.setTimeWasting(baseProfile.getTimeWasting());
        
        // Apply direct overrides
        modified.setPressingIntensity(plan.getPressingIntensity());
        modified.setDefensiveLine(plan.getDefensiveLine());
        modified.setAttackingWidth(plan.getAttackingWidth());
        modified.setCounterAttack(plan.getCounterAttack());
        modified.setOffsideTrap(plan.getOffsideTrap());
        
        // Map Tactical Approach Modifiers (These adjust derived behaviour!)
        switch (plan.getTacticalApproach()) {
            case ATTACKING -> {
                modified.setBuildUpStyle(BuildUpStyle.DIRECT);
                modified.setPassingRisk(Math.min(100, modified.getPassingRisk() + 20));
                modified.setAttackWidth(Math.min(100, modified.getAttackWidth() + 15));
            }
            case DEFENSIVE -> {
                modified.setBuildUpStyle(BuildUpStyle.SLOW_POSSESSION);
                modified.setPassingRisk(Math.max(1, modified.getPassingRisk() - 25));
                modified.setChanceCreation(ChanceCreation.POSSESSION);
                modified.setDefensiveWidth(Math.max(1, modified.getDefensiveWidth() - 20));
            }
            case POSSESSION -> {
                modified.setBuildUpStyle(BuildUpStyle.SLOW_POSSESSION);
                modified.setChanceCreation(ChanceCreation.POSSESSION);
                modified.setPassingRisk(Math.max(1, modified.getPassingRisk() - 15));
            }
            case COUNTER_ATTACK -> {
                modified.setBuildUpStyle(BuildUpStyle.DIRECT);
                modified.setChanceCreation(ChanceCreation.FAST_ATTACK);
                modified.setCounterAttack(true);
                modified.setDefensiveLine(Math.max(1, modified.getDefensiveLine() - 20));
            }
            case HIGH_PRESS -> {
                modified.setHighPress(true);
                modified.setPressingIntensity(Math.max(75, modified.getPressingIntensity() + 25));
                modified.setDefensiveLine(Math.min(100, modified.getDefensiveLine() + 20));
            }
            case LOW_BLOCK -> {
                modified.setHighPress(false);
                modified.setPressingIntensity(Math.max(1, modified.getPressingIntensity() - 30));
                modified.setDefensiveLine(Math.max(1, modified.getDefensiveLine() - 30));
                modified.setDefensiveWidth(Math.max(1, modified.getDefensiveWidth() - 25));
            }
            case BALANCED -> {}
        }
        
        // Tempo modifier mapping
        if (plan.getTempo() > 60) {
            modified.setChanceCreation(ChanceCreation.FAST_ATTACK);
            modified.setPassingRisk(Math.min(100, modified.getPassingRisk() + (plan.getTempo() - 50) / 2));
        } else if (plan.getTempo() < 40) {
            modified.setBuildUpStyle(BuildUpStyle.SLOW_POSSESSION);
        }
        
        return modified;
    }
}
