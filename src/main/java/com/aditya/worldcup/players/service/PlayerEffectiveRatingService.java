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
    private final com.aditya.worldcup.saves.repository.SaveSlotRepository saveSlotRepository;

    public int getEffectiveAge(Player player, com.aditya.worldcup.managers.entity.Manager manager) {
        if (manager == null) {
            return player.getAge();
        }
        var saves = saveSlotRepository.findByManagerIdAndActiveTrue(manager.getId());
        if (saves.isEmpty() || saves.get(0).getInitialSeason() == null) {
            return player.getAge();
        }
        int delta = saves.get(0).getCurrentSeason() - saves.get(0).getInitialSeason();
        return player.getAge() + delta;
    }

    public int calculate(Player player) {
        PlayerState state = playerStateService.getOrCreateState(player);
        return calculate(player, state);
    }

    public int calculate(Player player, PlayerState state) {
        if (!playerStateService.isAvailable(state)) {
            return 0;
        }

        int effectiveAge = getEffectiveAge(player, state.getManager());
        double declinePenalty = effectiveAge > 34 ? (effectiveAge - 34) * 1.5 : 0.0;

        double fitnessPenalty = state.getFitness() < 60 ? (60 - state.getFitness()) / 5.0 : 0.0;
        double fatiguePenalty = state.getFatigue() > 70 ? (state.getFatigue() - 70) / 5.0 : 0.0;

        double adjustment = state.getCurrentForm() * 0.5
                + (state.getConfidence() - 50) / 25.0
                + (state.getFitness() - 100) / 15.0
                - state.getFatigue() / 15.0
                - fitnessPenalty
                - fatiguePenalty
                + (state.getMorale() - 50) / 25.0
                + state.getDevelopmentRating()
                - declinePenalty;

        if (state.getInjuryStatus() == InjuryStatus.MINOR) {
            adjustment -= 2;
        }

        return Math.max(1, Math.min(100,
                (int) Math.round(player.getOverallRating() + adjustment)));
    }

    public EffectiveAttributes getEffectiveAttributes(Player player, PlayerState state) {
        return new EffectiveAttributes(
                clampToPotential(player.getPace() + state.getPaceDelta(), player.getPotential()),
                clampToPotential(player.getShooting() + state.getShootingDelta(), player.getPotential()),
                clampToPotential(player.getPassing() + state.getPassingDelta(), player.getPotential()),
                clampToPotential(player.getDribbling() + state.getDribblingDelta(), player.getPotential()),
                clampToPotential(player.getDefending() + state.getDefendingDelta(), player.getPotential()),
                clampToPotential(player.getPhysical() + state.getPhysicalDelta(), player.getPotential()),
                calculate(player, state),
                getEffectiveAge(player, state.getManager())
        );
    }

    private int clampToPotential(int effective, int potential) {
        return Math.max(1, Math.min(99, Math.min(effective, potential)));
    }

    public record EffectiveAttributes(
            int pace,
            int shooting,
            int passing,
            int dribbling,
            int defending,
            int physical,
            int overallRating,
            int age
    ) {}
}
