package com.aditya.worldcup.managers.service;

import com.aditya.worldcup.managers.entity.ManagerEvent;
import com.aditya.worldcup.managers.repository.ManagerRepository;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.repository.PlayerStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManagerConsequenceService {

    private final ManagerEconomyService economyService;
    private final PlayerStateRepository playerStateRepository;
    private final CareerHistoryService careerHistoryService;

    public String applyDecisionConsequences(ManagerEvent event) {
        String decision = event.getSelectedDecision();
        String resolutionText = "Decision applied.";

        switch (event.getType()) {
            case FEDERATION_RESOURCE_GRANT -> {
                String idempotencyKey = "EVENT_REWARD_" + event.getId() + "_" + decision;
                if (decision.equals("ALLOCATE_TRAINING")) {
                    economyService.addFunds(event.getManager(), 500, "Federation Grant: Training", idempotencyKey);
                    resolutionText = "Received 500 resources for Training.";
                } else if (decision.equals("ALLOCATE_MEDICAL")) {
                    economyService.addFunds(event.getManager(), 500, "Federation Grant: Medical", idempotencyKey);
                    resolutionText = "Received 500 resources for Medical.";
                } else if (decision.equals("ALLOCATE_SCOUTING")) {
                    economyService.addFunds(event.getManager(), 500, "Federation Grant: Scouting", idempotencyKey);
                    resolutionText = "Received 500 resources for Scouting.";
                }
            }
            case PLAYER_FATIGUE_WARNING -> {
                if (event.getRelatedPlayer() != null) {
                    Optional<PlayerState> optState = playerStateRepository.findByManagerIdAndPlayerId(event.getManager().getId(), event.getRelatedPlayer().getId());
                    if (optState.isPresent()) {
                        PlayerState state = optState.get();
                        if (decision.equals("REST_PLAYER")) {
                            state.setFitness(Math.min(100, state.getFitness() + 15));
                            state.setWorkload(Math.max(0, state.getWorkload() - 20));
                            resolutionText = "Player was rested. Fitness improved significantly, workload dropped.";
                        } else if (decision.equals("REDUCE_INTENSITY")) {
                            state.setFitness(Math.min(100, state.getFitness() + 5));
                            state.setWorkload(Math.max(0, state.getWorkload() - 10));
                            resolutionText = "Training intensity reduced for player. Modest fitness recovery.";
                        } else if (decision.equals("PLAY_THROUGH")) {
                            state.setWorkload(state.getWorkload() + 10);
                            resolutionText = "Player pushed through fatigue. Workload increased.";
                        }
                        playerStateRepository.save(state);
                    } else {
                        resolutionText = "Player state not found, consequence skipped.";
                    }
                }
            }
            case SQUAD_SELECTION_CONFLICT -> {
                if (decision.equals("BACK_VETERAN")) {
                    resolutionText = "You backed the veteran player, ensuring short-term squad stability.";
                } else {
                    resolutionText = "You promoted the youth prospect, looking toward long-term development.";
                }
            }
            case TACTICAL_PREPARATION_DILEMMA -> {
                if (decision.equals("DEFENSIVE_PREP")) {
                    resolutionText = "Squad preparation intensely focused on defensive shape.";
                } else {
                    resolutionText = "Squad preparation heavily emphasized attacking fluidity.";
                }
            }
            case PLAYER_TRAINING_BREAKTHROUGH -> {
                resolutionText = "Player morale and development improved organically.";
            }
        }
        return resolutionText;
    }
}
