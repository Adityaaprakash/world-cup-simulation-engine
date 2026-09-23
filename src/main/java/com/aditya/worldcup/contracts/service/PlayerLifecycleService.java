package com.aditya.worldcup.contracts.service;

import com.aditya.worldcup.contracts.entity.PlayerLifecycle;
import com.aditya.worldcup.contracts.repository.PlayerLifecycleRepository;
import com.aditya.worldcup.managers.entity.CareerTimelineEvent;
import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.TimelineEventType;
import com.aditya.worldcup.managers.repository.CareerTimelineEventRepository;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service to manage lifecycle of players.
 */
@Service
@RequiredArgsConstructor
public class PlayerLifecycleService {

    private final PlayerRepository playerRepository;
    private final PlayerLifecycleRepository playerLifecycleRepository;
    private final ManagerService managerService;
    private final CareerTimelineEventRepository careerTimelineEventRepository;

    /**
     * Retires a given player.
     * @param playerId the player id
     * @param authentication the user session
     * @return the retired player entity
     */
    @Transactional
    public Player retirePlayer(Long playerId, Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        PlayerLifecycle lifecycle = playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), playerId)
                .orElseGet(() -> PlayerLifecycle.builder()
                        .manager(manager)
                        .player(player)
                        .build());

        if (Boolean.TRUE.equals(lifecycle.getRetired())) {
            throw new IllegalStateException("Player is already retired.");
        }

        lifecycle.setRetired(true);
        lifecycle.setActive(false);
        lifecycle.setUpdatedAt(LocalDateTime.now());
        playerLifecycleRepository.save(lifecycle);

        recordEvent(manager, TimelineEventType.PLAYER_RETIRED, "Player Retired", player.getName() + " has retired from international football.");
        
        return player;
    }

    /**
     * Reactivates a given retired player.
     * @param playerId the player id
     * @param authentication the user session
     * @return the reactivated player entity
     */
    @Transactional
    public Player reactivatePlayer(Long playerId, Authentication authentication) {
        Manager manager = managerService.getOrCreateManager(authentication);
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        PlayerLifecycle lifecycle = playerLifecycleRepository.findByManagerIdAndPlayerId(manager.getId(), playerId)
                .orElseGet(() -> PlayerLifecycle.builder()
                        .manager(manager)
                        .player(player)
                        .build());

        if (!Boolean.TRUE.equals(lifecycle.getRetired())) {
            throw new IllegalStateException("Player is not retired.");
        }

        lifecycle.setRetired(false);
        lifecycle.setActive(true);
        lifecycle.setUpdatedAt(LocalDateTime.now());
        playerLifecycleRepository.save(lifecycle);

        recordEvent(manager, TimelineEventType.PLAYER_REACTIVATED, "Player Reactivated", player.getName() + " has come out of retirement.");
        
        return player;
    }

    private void recordEvent(Manager manager, TimelineEventType type, String title, String description) {
        careerTimelineEventRepository.save(CareerTimelineEvent.builder()
                .manager(manager)
                .eventType(type)
                .title(title)
                .description(description)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
