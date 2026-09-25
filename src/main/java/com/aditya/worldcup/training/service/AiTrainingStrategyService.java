package com.aditya.worldcup.training.service;

import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.service.TournamentIntelligenceService;
import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI manager training strategies based on current tournament momentum.
 */
@Service
@RequiredArgsConstructor
public class AiTrainingStrategyService {

    private final PlayerTrainingService playerTrainingService;
    private final TournamentIntelligenceService tournamentIntelligenceService;
    
    @Transactional
    public void executeAiTrainingStrategy(Long squadId, Team team, Long tournamentId) {
        String form = tournamentIntelligenceService.formForTeam(tournamentId, team.getId());
        double momentum = tournamentIntelligenceService.momentumForTeam(tournamentId, team.getId());
        
        TrainingCategory category = TrainingCategory.TACTICAL;
        TrainingIntensity intensity = TrainingIntensity.NORMAL;
        
        // Strategy: High momentum teams train rest and light technical to prevent fatigue
        if (momentum > 0.7 || form.contains("WW")) {
            category = TrainingCategory.REST;
            intensity = TrainingIntensity.LIGHT;
        } 
        // Strategy: Teams losing momentum aggressively train tactics
        else if (form.contains("L") || momentum < 0.3) {
            category = TrainingCategory.TACTICAL;
            intensity = TrainingIntensity.INTENSE;
        } 
        // Strategy: Neutral teams focus on physical fitness
        else {
            category = TrainingCategory.PHYSICAL;
            intensity = TrainingIntensity.NORMAL;
        }
        
        playerTrainingService.trainSquad(squadId, category, intensity);
    }
}
