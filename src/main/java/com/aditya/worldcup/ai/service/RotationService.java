package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RotationService {

    private final PlayerStateService playerStateService;

    public boolean isAvailable(Player player) {
        return playerStateService.isAvailable(playerStateService.getOrCreateState(player));
    }

    public boolean shouldRest(Player player) {
        PlayerState state = playerStateService.getOrCreateState(player);
        return state.getFitness() < 65
                || state.getFatigue() > 70
                || (state.getFatigue() > 55 && isHighRotationPosition(player));
    }

    public double availabilityScore(Player player) {
        PlayerState state = playerStateService.getOrCreateState(player);
        double ageRotationPenalty = player.getAge() >= 33 ? 2.0 : 0.0;
        return state.getFitness() * 0.08 - state.getFatigue() * 0.1
                - ageRotationPenalty;
    }

    private boolean isHighRotationPosition(Player player) {
        return switch (player.getPosition()) {
            case RB, LB, RW, LW -> true;
            default -> false;
        };
    }
}
