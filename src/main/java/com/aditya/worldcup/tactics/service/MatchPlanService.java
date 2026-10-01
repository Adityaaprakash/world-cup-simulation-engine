package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.tactics.entity.BuildUpStyle;

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
        modified.setPressingIntensity(baseProfile.getPressingIntensity());
        modified.setDefensiveLine(baseProfile.getDefensiveLine());
        modified.setTempo(baseProfile.getTempo());
        modified.setWidth(baseProfile.getWidth());
        modified.setPassingStyle(baseProfile.getPassingStyle());
        modified.setAttackingApproach(baseProfile.getAttackingApproach());
        modified.setBuildUpStyle(baseProfile.getBuildUpStyle());
        modified.setDefensiveBlock(baseProfile.getDefensiveBlock());
        
        // Map Tactical Approach Modifiers
        switch (plan.getTacticalApproach()) {
            case ATTACKING -> {
                modified.setBuildUpStyle(BuildUpStyle.DIRECT);
                modified.setPassingStyle(com.aditya.worldcup.tactics.entity.PassingStyle.DIRECT);
                modified.setWidth(com.aditya.worldcup.tactics.entity.Width.WIDE);
            }
            case DEFENSIVE -> {
                modified.setBuildUpStyle(BuildUpStyle.POSSESSION);
                modified.setPassingStyle(com.aditya.worldcup.tactics.entity.PassingStyle.SHORT);
                modified.setWidth(com.aditya.worldcup.tactics.entity.Width.NARROW);
            }
            case POSSESSION -> {
                modified.setBuildUpStyle(BuildUpStyle.POSSESSION);
                modified.setPassingStyle(com.aditya.worldcup.tactics.entity.PassingStyle.SHORT);
            }
            case COUNTER_ATTACK -> {
                modified.setBuildUpStyle(BuildUpStyle.DIRECT);
                modified.setDefensiveBlock(com.aditya.worldcup.tactics.entity.DefensiveBlock.LOW_BLOCK);
                modified.setTempo(com.aditya.worldcup.tactics.entity.Tempo.FAST);
            }
            case HIGH_PRESS -> {
                modified.setPressingIntensity(com.aditya.worldcup.tactics.entity.PressingIntensity.HIGH);
                modified.setDefensiveLine(com.aditya.worldcup.tactics.entity.DefensiveLine.HIGH);
            }
            case LOW_BLOCK -> {
                modified.setPressingIntensity(com.aditya.worldcup.tactics.entity.PressingIntensity.LOW);
                modified.setDefensiveLine(com.aditya.worldcup.tactics.entity.DefensiveLine.DEEP);
            }
            case BALANCED -> {}
        }
        
        return modified;
    }
}
