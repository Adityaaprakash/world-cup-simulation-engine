package com.aditya.worldcup.training.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.squadplayers.repository.SquadPlayerRepository;
import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
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
    private final SquadPlayerRepository squadPlayerRepository;

    @Transactional
    public void trainSquad(Long squadId, TrainingCategory category, TrainingIntensity intensity) {
        List<SquadPlayer> players = squadPlayerRepository.findBySquadId(squadId);
        
        List<Player> playerList = players.stream()
                .map(SquadPlayer::getPlayer)
                .collect(Collectors.toList());
                
        trainPlayers(playerList, category, intensity);
    }
    
    @Transactional
    public void trainPlayers(List<Player> players, TrainingCategory category, TrainingIntensity intensity) {
        if (players == null || players.isEmpty()) {
            return;
        }

        List<PlayerState> states = players.stream()
                .map(playerStateService::getOrCreateState)
                .collect(Collectors.toList());

        for (PlayerState state : states) {
            processPlayerTraining(state, category, intensity);
        }

        playerStateService.saveAll(states);
    }

    public void processPlayerTraining(PlayerState state, TrainingCategory category, TrainingIntensity intensity) {
        
        boolean isAvailable = playerStateService.isAvailable(state);
        
        if (category == TrainingCategory.REST) {
            int newFatigue = Math.max(0, state.getFatigue() - 20);
            state.setFatigue(newFatigue);
            state.setMorale(Math.min(100, state.getMorale() + 2));
            return;
        }

        // Injured/unavailable players cannot train (unless resting, which is handled above)
        if (!isAvailable) {
            return;
        }

        // Workload & fatigue interactions
        int fatigueIncrease = switch (intensity) {
            case LIGHT -> 5;
            case NORMAL -> 12;
            case INTENSE -> 25;
        };

        int currentFatigue = state.getFatigue();
        // High fatigue penalties starting from 50
        double fatiguePenalty = Math.max(0.0, (currentFatigue - 50) / 50.0);
        
        // Progression
        int baseProgression = calculateBaseProgression(state.getPlayer(), intensity, category);
        int actualProgression = (int) Math.round(baseProgression * (1.0 - fatiguePenalty));
        
        // Morale and fitness interactions
        applySecondaryEffects(state, intensity);

        // Update fatigue
        int newFatigue = Math.min(100, state.getFatigue() + fatigueIncrease);
        state.setFatigue(newFatigue);

        // Update development
        applyProgression(state, actualProgression);
    }

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
        
        // Age curve
        if (age < 23) {
            // Young players grow fast
            return Math.max(1, (roomToGrow > 0 ? roomToGrow : 1) * intensityMultiplier);
        } else if (age < 30) {
            // Peak age, slower growth
            return Math.max(1, (roomToGrow > 0 ? roomToGrow / 2 : 0) * intensityMultiplier);
        } else {
            // Older players decline depending on intensity, or very slow growth if below potential
            if (intensity == TrainingIntensity.INTENSE) {
                // Intense training speeds up decline for old players
                return -5;
            } else if (intensity == TrainingIntensity.NORMAL) {
                return -2;
            } else {
                // Light training might maintain
                return roomToGrow > 0 ? 1 : 0;
            }
        }
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

    private void applyProgression(PlayerState state, int amount) {
        if (amount == 0) {
            return;
        }
        
        int currentTracker = state.getProgressionTracker() + amount;
        int currentRating = state.getDevelopmentRating();
        
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
    }
}
