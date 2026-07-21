package com.aditya.worldcup.players.service;

import com.aditya.worldcup.players.entity.InjuryStatus;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerEffectiveRatingService {

    private final PlayerStateService playerStateService;

    public int calculate(Player player) {
        PlayerState state = playerStateService.getOrCreateState(player);
        return calculate(player, state);
    }

    public int calculate(Player player, PlayerState state) {
        if (!playerStateService.isAvailable(state)) {
            return 0;
        }

        double adjustment = state.getCurrentForm() * 0.4
                + (state.getConfidence() - 50) / 25.0
                + (state.getFitness() - 100) / 20.0
                - state.getFatigue() / 25.0
                + (state.getMorale() - 50) / 25.0;

        if (state.getInjuryStatus() == InjuryStatus.MINOR) {
            adjustment -= 2;
        }

        return Math.max(1, Math.min(100,
                (int) Math.round(player.getOverallRating() + adjustment)));
    }
}
