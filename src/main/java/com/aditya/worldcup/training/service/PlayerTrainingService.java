package com.aditya.worldcup.training.service;

import com.aditya.worldcup.managers.entity.ManagerEconomy;
import com.aditya.worldcup.managers.repository.ManagerEconomyRepository;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.squads.repository.SquadRepository;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.service.ManagerObjectiveService;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.managers.service.ManagerEventGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerTrainingService {

    private static final int MAX_DEVELOPMENT = 10;
    private static final int MIN_DEVELOPMENT = -5;

    private final PlayerStateService playerStateService;
    private final PlayerRepository playerRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final SquadRepository squadRepository;
    private final ManagerEconomyRepository managerEconomyRepository;
    private final ManagerObjectiveService managerObjectiveService;
    private final ManagerService managerService;
    private final ManagerEventGeneratorService eventGeneratorService;

    @Transactional
    public void trainSquad(Long squadId, TrainingCategory category, TrainingIntensity intensity) {
        Squad squad = squadRepository.findById(squadId).orElseThrow();
        ManagerEconomy economy = managerEconomyRepository.findByManagerId(squad.getUser().getId()).orElse(null);
        
        List<SquadPlayer> players = squadPlayerRepository.findBySquadId(squadId);
        
        List<Player> playerList = players.stream()
                .map(SquadPlayer::getPlayer)
                .collect(Collectors.toList());
        Manager manager = null;
        if (squad.getUser() != null && squad.getUser().getEmail() != null) {
            manager = managerService.getOrCreateManager(squad.getUser().getEmail());
        }
        trainPlayersWithEconomy(playerList, category, intensity, economy, manager);
    }

    @Transactional
    public void trainPlayers(List<Player> players, TrainingCategory category, TrainingIntensity intensity) {
        trainPlayersWithEconomy(players, category, intensity, null, null);
    }
    
    @Transactional
    public void trainPlayersWithEconomy(List<Player> players, TrainingCategory category, TrainingIntensity intensity, ManagerEconomy economy, Manager manager) {
        if (players == null || players.isEmpty()) {
            return;
        }

        List<PlayerState> states = players.stream()
                .map(playerStateService::getOrCreateState)
                .collect(Collectors.toList());

        for (PlayerState state : states) {
            processPlayerTraining(state, category, intensity, economy, manager);
        }

        playerStateService.saveAll(states);
        
        eventGeneratorService.generatePostTrainingEvents(manager, states);
    }

    public void processPlayerTraining(PlayerState state, TrainingCategory category, TrainingIntensity intensity, ManagerEconomy economy, Manager manager) {
        
        boolean isAvailable = playerStateService.isAvailable(state);
        
        int medicalBonus = 0;
        int trainingBonus = 0;
        if (economy != null) {
            // max 20% bonus from investments
            medicalBonus = Math.min(20, economy.getMedicalAllocation() / 5);
            trainingBonus = Math.min(20, economy.getTrainingAllocation() / 5);
        }
        
        if (category == TrainingCategory.REST) {
            int baseFatigueRecovery = 20;
            int fatigueRecovery = baseFatigueRecovery + (baseFatigueRecovery * medicalBonus / 100);
            
            int newFatigue = Math.max(0, state.getFatigue() - fatigueRecovery);
            int newWorkload = Math.max(0, state.getWorkload() - 15);
            state.setFatigue(newFatigue);
            state.setWorkload(newWorkload);
            state.setMorale(Math.min(100, state.getMorale() + (2 + medicalBonus / 10)));
            return;
        }

        // Injured/unavailable players cannot train (unless resting, which is handled above)
        if (!isAvailable) {
            return;
        }

        // Workload & fatigue interactions
        int workloadIncrease = switch (intensity) {
            case LIGHT -> 8;
            case NORMAL -> 15;
            case INTENSE -> 25;
        };
        
        int currentWorkload = state.getWorkload();
        int newWorkload = Math.min(100, currentWorkload + workloadIncrease);
        state.setWorkload(newWorkload);
        
        double workloadMultiplier = currentWorkload > 70 ? 1.5 : 1.0;
        
        int fatigueIncrease = (int) Math.round(switch (intensity) {
            case LIGHT -> 5;
            case NORMAL -> 12;
            case INTENSE -> 25;
        } * workloadMultiplier);

        int currentFatigue = state.getFatigue();
        // High fatigue penalties starting from 50
        double fatiguePenalty = Math.min(1.0, currentFatigue * FATIGUE_PENALTY_PER_POINT);
        
        // Progression
        int baseProgression = calculateBaseProgression(state.getPlayer(), intensity, category);
        int finalProgressionBase = baseProgression + (baseProgression * trainingBonus / 100);
        int actualProgression = (int) Math.round(finalProgressionBase * (1.0 - fatiguePenalty));
        
        // Morale and fitness interactions
        applySecondaryEffects(state, intensity);

        // Update fatigue
        int newFatigue = Math.min(100, state.getFatigue() + fatigueIncrease);
        state.setFatigue(newFatigue);

        // --- Phase 14C: POSITION training — mutate isolated PlayerState deltas ONLY if they are progressing natively ---
        if (category == TrainingCategory.POSITION && actualProgression > 0) {
            Player player = state.getPlayer();
            PositionTrainingUtil.applyPositionAttributeBoost(player, state, intensity);
            // No need to save player manually, as state holds the progression and is saved upstream
        }

        // Update development
        applyProgression(state, actualProgression, manager);
    }

    private static final double FATIGUE_PENALTY_PER_POINT = 0.02;

    private int calculateBaseProgression(Player player, TrainingIntensity intensity, TrainingCategory category) {
        int age = player.getAge();
        int potential = player.getPotential();
        int overall = player.getOverallRating();
        
        // Gap to potential
        int roomToGrow = potential - overall;
        
        // Intensity multiplier
        int intensityMultiplier = switch (intensity) {
            case LIGHT -> 1;
            case NORMAL -> 3;
            case INTENSE -> 6;
        };
        
        if (category == TrainingCategory.PHYSICAL) {
            // Physical training slightly more effective for young players
            intensityMultiplier += 1;
        }
        
        // Age curve refinement
        if (age < 20) {
            // Very young, extra growth boost
            intensityMultiplier += 2;
        } else if (age <= 22) {
            // Fast development still
            intensityMultiplier += 1;
        } else if (age <= 27) {
            // Development slows
            intensityMultiplier -= 1;
        } else if (age <= 30) {
            // Plateau / prime – no change
        } else if (age <= 34) {
            // Early decline, harder to improve
            intensityMultiplier -= 2;
        } else {
            // Over 34, natural decline
            if (intensity == TrainingIntensity.INTENSE) {
                return -5; // strong decline
            } else if (intensity == TrainingIntensity.NORMAL) {
                return -2; // moderate decline
            } else {
                // Light training may maintain or give tiny growth if below potential
                return roomToGrow > 0 ? 1 : 0;
            }
        }

        // Position specific training bonus
        if (category == TrainingCategory.POSITION) {
            int positionBonus = PositionTrainingUtil.positionBonus(player.getPosition(), intensity);
            intensityMultiplier += positionBonus;
        }

        // No positive growth if at or above potential
        if (roomToGrow <= 0) {
            return 0;
        }

        // Base progression respecting potential gap
        int base = Math.max(1, roomToGrow * intensityMultiplier);
        return base;
    }

    private void applySecondaryEffects(PlayerState state, TrainingIntensity intensity) {
        switch (intensity) {
            case LIGHT -> state.setMorale(Math.min(100, state.getMorale() + 1));
            case INTENSE -> state.setMorale(Math.max(0, state.getMorale() - 2)); 
            default -> {}
        }
        
        // Training maintains fitness
        if (intensity != TrainingIntensity.LIGHT) {
            state.setFitness(Math.min(100, state.getFitness() + 2));
        }
    }

    private void applyProgression(PlayerState state, int amount, Manager manager) {
        if (amount == 0) {
            return;
        }
        
        int currentTracker = state.getProgressionTracker() + amount;
        int currentRating = state.getDevelopmentRating();
        int initialRating = currentRating;
        
        while (currentTracker >= 100 && currentRating < MAX_DEVELOPMENT) {
            currentTracker -= 100;
            currentRating++;
        }
        
        while (currentTracker <= -100 && currentRating > MIN_DEVELOPMENT) {
            currentTracker += 100;
            currentRating--;
        }
        
        // Bounding tracker
        if (currentRating == MAX_DEVELOPMENT && currentTracker > 0) {
            currentTracker = 0;
        } else if (currentRating == MIN_DEVELOPMENT && currentTracker < 0) {
            currentTracker = 0;
        }
        
        state.setProgressionTracker(currentTracker);
        state.setDevelopmentRating(currentRating);

        if (manager != null && currentRating > initialRating) {
            managerObjectiveService.evaluatePlayerDevelopmentProgress(manager);
        }
    }
    
    @Transactional(readOnly = true)
    public double getAverageSquadWorkload(Long squadId) {
        return squadPlayerRepository.findBySquadId(squadId).stream()
                .map(SquadPlayer::getPlayer)
                .map(playerStateService::getOrCreateState)
                .mapToInt(state -> state.getWorkload() != null ? state.getWorkload() : 0)
                .average()
                .orElse(0.0);
    }
}
