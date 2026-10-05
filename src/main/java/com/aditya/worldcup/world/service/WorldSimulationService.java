package com.aditya.worldcup.world.service;

import com.aditya.worldcup.contracts.entity.PlayerLifecycle;
import com.aditya.worldcup.contracts.repository.PlayerLifecycleRepository;
import com.aditya.worldcup.contracts.service.PlayerContractService;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.repository.CareerTimelineEventRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.repository.PlayerRepository;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.repository.SaveSlotRepository;
import com.aditya.worldcup.training.entity.TrainingCategory;
import com.aditya.worldcup.training.entity.TrainingIntensity;
import com.aditya.worldcup.training.service.PlayerTrainingService;
import com.aditya.worldcup.countries.entity.Country;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldSimulationService {

    private final ManagerService managerService;
    private final SaveSlotRepository saveSlotRepository;
    private final PlayerContractService playerContractService;
    private final PlayerLifecycleRepository playerLifecycleRepository;
    private final PlayerRepository playerRepository;
    private final PlayerEffectiveRatingService playerEffectiveRatingService;
    private final PlayerTrainingService playerTrainingService;
    private final PlayerStateRepository playerStateRepository;
    private final EntityManager entityManager;

    private static final int RETIREMENT_AGE_MIN = 34;
    private static final int RETIREMENT_AGE_MAX = 40;

    // Deterministic random using save seed for youth generation
    private final Random random = new Random();

    @Transactional
    public SaveSlot advanceWorldTick(Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);

        SaveSlot activeSlot = saveSlotRepository.findByManagerIdAndActiveTrue(manager.getId())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No active save slot found for manager"));

        log.info("Advancing world tick for manager {} from season {}", manager.getId(), activeSlot.getCurrentSeason());

        // 1. Process contract expirations (using existing logic)
        playerContractService.evaluateExpiries(authentication);

        // Advance to next season
        int newSeason = activeSlot.getCurrentSeason() + 1;
        activeSlot.setCurrentSeason(newSeason);

        // Save to make sure effectiveAge calculations are immediately updated for subsequent steps
        activeSlot = saveSlotRepository.save(activeSlot);

        // 2. Process Age, Development & Retirements for all known generated/tracked players
        processWorldPlayerEvolution(manager, activeSlot);

        // 3. Generate youth players to replenish the world
        generateYouthPlayers(manager);

        return activeSlot;
    }

    private void processWorldPlayerEvolution(Manager manager, SaveSlot activeSlot) {
        // Find players we care about (this would typically be players in squads or tracking lists)
        // For efficiency, we load player states that exist for this manager
        List<PlayerState> states = playerStateRepository.findByManagerId(manager.getId());

        for (PlayerState state : states) {
            Player player = state.getPlayer();
            int effectiveAge = playerEffectiveRatingService.getEffectiveAge(player, manager);

            // Check retirement
            if (!isRetired(player, manager)) {
                if (shouldRetire(effectiveAge)) {
                    retirePlayer(player, manager);
                    continue; // Skip training if they retire
                }

                // Process natural baseline evolution (yearly development)
                // We simulate a light physical training as baseline evolution if they don't have matches
                playerTrainingService.processPlayerTraining(state, TrainingCategory.PHYSICAL, TrainingIntensity.LIGHT, null, manager);
            }
        }

        playerStateRepository.saveAll(states);
    }

    private boolean isRetired(Player player, Manager manager) {
        return playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), player.getId())
                .map(PlayerLifecycle::getRetired)
                .orElse(player.getRetired()); // fallback to global baseline
    }

    private boolean shouldRetire(int age) {
        if (age >= RETIREMENT_AGE_MAX) return true;
        if (age < RETIREMENT_AGE_MIN) return false;

        // Increasing probability of retirement as they age
        double probability = (age - RETIREMENT_AGE_MIN) / (double)(RETIREMENT_AGE_MAX - RETIREMENT_AGE_MIN);
        return random.nextDouble() < probability;
    }

    private void retirePlayer(Player player, Manager manager) {
        PlayerLifecycle lifecycle = playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), player.getId())
                .orElseGet(() -> PlayerLifecycle.builder()
                        .manager(manager)
                        .player(player)
                        .build());
        lifecycle.setRetired(true);
        lifecycle.setActive(false);
        lifecycle.setUpdatedAt(LocalDateTime.now());
        playerLifecycleRepository.save(lifecycle);
        log.info("Player {} retired at effective age {}", player.getName(), playerEffectiveRatingService.getEffectiveAge(player, manager));
    }

    private void generateYouthPlayers(Manager manager) {
        // Generate new players for countries to maintain the ecosystem.
        List<Country> countries = entityManager.createQuery("SELECT c FROM Country c", Country.class).getResultList();

        for (Country country : countries) {
            // Count active, unretired players for this country that are accessible to this manager
            // Simple heuristic to ensure enough young talent

            // Generate 1-2 youth players per country per season
            int numYouth = 1 + random.nextInt(2);
            for (int i=0; i<numYouth; i++) {
                PlayerPosition position = PlayerPosition.values()[random.nextInt(PlayerPosition.values().length)];

                int potential = 60 + random.nextInt(35); // 60 to 95 potential
                int overall = Math.max(40, potential - (15 + random.nextInt(20))); // Much lower than potential

                Player youth = Player.builder()
                        .country(country)
                        .name("Youth " + country.getFifaCode() + " " + random.nextInt(10000))
                        .age(16 + random.nextInt(3)) // 16-18
                        .position(position)
                        .overallRating(overall)
                        .potential(potential)
                        .pace(generateAttribute(overall))
                        .shooting(generateAttribute(overall))
                        .passing(generateAttribute(overall))
                        .dribbling(generateAttribute(overall))
                        .defending(generateAttribute(overall))
                        .physical(generateAttribute(overall))
                        .marketValue(100000L + (long)random.nextInt(500000))
                        .active(true)
                        .retired(false)
                        .preferredFoot(random.nextBoolean() ? "RIGHT" : "LEFT")
                        .generatedForManager(manager) // Phase 14E Isolation
                        .build();

                playerRepository.save(youth);
            }
        }
    }

    private int generateAttribute(int overall) {
        // Generate an attribute around the overall rating
        int variance = 10;
        int attr = overall + (random.nextInt(variance * 2) - variance);
        return Math.max(1, Math.min(99, attr));
    }
}
