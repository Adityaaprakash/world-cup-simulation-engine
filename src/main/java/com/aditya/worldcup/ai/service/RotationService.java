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
        return shouldRest(player, MatchImportance.GROUP_STAGE);
    }

    public boolean shouldRest(Player player, MatchImportance importance) {
        PlayerState state = playerStateService.getOrCreateState(player);
        int fitnessFloor = switch (importance) {
            case FINAL -> 55;
            case SEMI_FINAL -> 58;
            case KNOCKOUT -> 62;
            case GROUP_STAGE -> 68;
        };
        int fatigueCeiling = switch (importance) {
            case FINAL -> 84;
            case SEMI_FINAL -> 80;
            case KNOCKOUT -> 74;
            case GROUP_STAGE -> 66;
        };
        return state.getFitness() < fitnessFloor
                || state.getFatigue() > fatigueCeiling
                || (state.getFatigue() > fatigueCeiling - 15
                && isHighRotationPosition(player)
                && importance != MatchImportance.FINAL);
    }

    public double availabilityScore(Player player) {
        return availabilityScore(player, MatchImportance.GROUP_STAGE);
    }

    public double availabilityScore(Player player, MatchImportance importance) {
        PlayerState state = playerStateService.getOrCreateState(player);
        double ageRotationPenalty = player.getAge() >= 33 ? 2.0 : 0.0;
        return state.getFitness() * 0.08
                - state.getFatigue() * 0.1 * importance.rotationWeight()
                - ageRotationPenalty * importance.rotationWeight();
    }

    private boolean isHighRotationPosition(Player player) {
        return switch (player.getPosition()) {
            case RB, LB, RW, LW -> true;
            default -> false;
        };
    }
}
