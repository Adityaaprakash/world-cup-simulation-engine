package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService;
import com.aditya.worldcup.players.service.PlayerStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerEvaluationService {

    private final PlayerEffectiveRatingService playerEffectiveRatingService;
    private final PlayerStateService playerStateService;

    public double evaluatePlayer(Player player) {
        return evaluatePlayer(player, player.getPosition());
    }

    public double evaluatePlayer(Player player, PlayerPosition targetPosition) {
        PlayerState state = playerStateService.getOrCreateState(player);
        if (!playerStateService.isAvailable(state)) {
            return 0;
        }

        double positionFit = player.getPosition() == targetPosition ? 2.0 : -3.0;
        double experience = Math.min(4.0, Math.max(0, player.getAge() - 21) * 0.18);
        double workloadPenalty = state.getFatigue() * 0.1
                + Math.max(0, 65 - state.getFitness()) * 0.14;

        return playerEffectiveRatingService.calculate(player, state)
                + player.getOverallRating() * 0.08
                + state.getCurrentForm() * 0.45
                + (state.getConfidence() - 50) * 0.035
                + (state.getMorale() - 50) * 0.03
                + state.getFitness() * 0.035
                + positionFit
                + experience
                - workloadPenalty;
    }
}
