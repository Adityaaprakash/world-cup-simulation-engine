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

        double fitnessPenalty = state.getFitness() < 60 ? (60 - state.getFitness()) / 5.0 : 0.0;
        double fatiguePenalty = state.getFatigue() > 70 ? (state.getFatigue() - 70) / 5.0 : 0.0;

        double adjustment = state.getCurrentForm() * 0.5
                + (state.getConfidence() - 50) / 25.0
                + (state.getFitness() - 100) / 15.0
                - state.getFatigue() / 15.0
                - fitnessPenalty
                - fatiguePenalty
                + (state.getMorale() - 50) / 25.0
                + state.getDevelopmentRating();

        if (state.getInjuryStatus() == InjuryStatus.MINOR) {
            adjustment -= 2;
        }

        return Math.max(1, Math.min(100,
                (int) Math.round(player.getOverallRating() + adjustment)));
    }
}
