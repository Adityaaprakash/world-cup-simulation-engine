package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.managers.entity.ManagerEvent;
import com.aditya.worldcup.managers.entity.ManagerEventStatus;
import com.aditya.worldcup.managers.entity.ManagerEventType;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerEventGeneratorService {

    private final ManagerEventService eventService;
    private final PlayerStateService playerStateService;

    public void generatePostTrainingEvents(Manager manager, List<PlayerState> trainedStates) {
        if (manager == null) return;
        
        for (PlayerState state : trainedStates) {
            if (state.getFatigue() > 80 && state.getWorkload() > 85) {
                String contextId = "FATIGUE_" + state.getPlayer().getId() + "_" + LocalDateTime.now().toLocalDate();
                ManagerEvent event = ManagerEvent.builder()
                        .manager(manager)
                        .type(ManagerEventType.PLAYER_FATIGUE_WARNING)
                        .title("Player Fatigue Warning: " + state.getPlayer().getName())
                        .description("Medical staff are concerned about the extreme fatigue level of " + state.getPlayer().getName() + ". Approaching critical threshold.")
                        .contextId(contextId)
                        .status(ManagerEventStatus.PENDING)
                        .relatedPlayer(state.getPlayer())
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusDays(3))
                        .build();
                eventService.generateEventIfNotExists(event);
            }
            if (state.getMorale() > 95 && state.getProgressionTracker() > 50) {
                String contextId = "BREAKTHROUGH_" + state.getPlayer().getId() + "_" + LocalDateTime.now().toLocalDate();
                ManagerEvent event = ManagerEvent.builder()
                        .manager(manager)
                        .type(ManagerEventType.PLAYER_TRAINING_BREAKTHROUGH)
                        .title("Training Breakthrough: " + state.getPlayer().getName())
                        .description(state.getPlayer().getName() + " has shown exceptional progress and attitude in recent training sessions.")
                        .contextId(contextId)
                        .status(ManagerEventStatus.PENDING)
                        .relatedPlayer(state.getPlayer())
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusDays(3))
                        .build();
                eventService.generateEventIfNotExists(event);
            }
        }
    }

    public void generatePostMatchEvents(Match match, Manager manager) {
        if (manager == null || match == null) return;
        
        // Example deterministic event: if it's the finals or something, but let's just do a federation grant if objective implies it
        // Or SQUAD_SELECTION_CONFLICT occasionally. For deterministic behavior without randomness:
        // We trigger a tactical dilemma before important matches, but this is post-match.
        
        // SQUAD_SELECTION_CONFLICT if a young player played well? We don't have access to ratings here easily unless we fetch them.
        
        // Let's create an intermittent Federation Resource Grant
        if (match.getId() % 3 == 0) {
            String contextId = "FEDERATION_GRANT_MATCH_" + match.getId();
            ManagerEvent event = ManagerEvent.builder()
                    .manager(manager)
                    .type(ManagerEventType.FEDERATION_RESOURCE_GRANT)
                    .title("Federation Resource Grant")
                    .description("The National Federation has authorized an additional resource grant following recent activities. Please allocate it.")
                    .contextId(contextId)
                    .status(ManagerEventStatus.PENDING)
                    .relatedMatch(match)
                    .relatedTournament(match.getTournament())
                    .createdAt(LocalDateTime.now())
                    .build();
            eventService.generateEventIfNotExists(event);
        }
    }
}
