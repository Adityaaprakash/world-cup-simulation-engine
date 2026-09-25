package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.simulation.dto.TeamStrengthResponse;
import com.aditya.worldcup.simulation.service.TeamStrengthService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.tactics.dto.MatchPlanDto;
import com.aditya.worldcup.tactics.dto.MatchPreparationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpponentAnalysisService {

    private final MatchRepository matchRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final TeamStrengthService teamStrengthService;
    private final MatchPlanService matchPlanService;
    private final PlayerStateService playerStateService;

    @Transactional(readOnly = true)
    public MatchPreparationResponse analyzePreparation(Long matchId, Long squadId, Long managerId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        Squad ownSquad = squadRepository.findById(squadId)
                .orElseThrow(() -> new IllegalArgumentException("Squad not found"));
        
        Squad opponentSquad;
        if (match.getHomeTeam().getId().equals(ownSquad.getTeam().getId())) {
            opponentSquad = squadRepository.findFirstByTeamId(match.getAwayTeam().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Opponent squad not found"));
        } else {
            opponentSquad = squadRepository.findFirstByTeamId(match.getHomeTeam().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Opponent squad not found"));
        }
        
        TeamStrengthResponse opponentStrength = teamStrengthService.calculateStrength(opponentSquad.getId());
        
        List<String> warnings = generateFitnessWarnings(ownSquad);
        MatchPlanDto currentPlan = matchPlanService.getMatchPlan(matchId, squadId, managerId)
                .map(matchPlanService::mapToDto)
                .orElse(null);
                
        return MatchPreparationResponse.builder()
                .matchId(matchId)
                .opponentSquadId(opponentSquad.getId())
                .opponentName(opponentSquad.getName())
                .opponentFormation(opponentSquad.getFormation().getName())
                .opponentStrength(opponentStrength)
                .fitnessWarnings(warnings)
                .currentPlan(currentPlan)
                .build();
    }
    
    private List<String> generateFitnessWarnings(Squad squad) {
        List<String> warnings = new ArrayList<>();
        List<SquadPlayer> players = squadPlayerRepository.findBySquadId(squad.getId());
        for (SquadPlayer sp : players) {
            if (Boolean.TRUE.equals(sp.getStartingXi())) {
                PlayerState state = playerStateService.getOrCreateState(sp.getPlayer());
                if (!sp.getPlayer().getActive() || sp.getPlayer().getRetired()) {
                    warnings.add(sp.getPlayer().getName() + " is currently unavailable/retired.");
                } else if (!playerStateService.isAvailable(state)) {
                    warnings.add(sp.getPlayer().getName() + " is currently injured.");
                } else {
                    if (state.getFitness() != null && state.getFitness() < 70) {
                        warnings.add(sp.getPlayer().getName() + " has low fitness (" + state.getFitness() + "%).");
                    }
                    if (state.getFatigue() != null && state.getFatigue() > 75) {
                        warnings.add(sp.getPlayer().getName() + " is highly fatigued.");
                    }
                    if (state.getWorkload() != null && state.getWorkload() > 80) {
                        warnings.add(sp.getPlayer().getName() + " has dangerously high workload.");
                    }
                }
            }
        }
        return warnings;
    }
}
