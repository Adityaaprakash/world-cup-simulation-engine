package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.ChanceCreation;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.service.TacticalProfileService;
import com.aditya.worldcup.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TacticalSelectionService {

    private final TacticalProfileService tacticalProfileService;
    private final PlayerStateService playerStateService;

    public TacticalProfile selectTactics(Team team, int squadQuality,
                                         int opponentQuality) {
        return selectTactics(team, List.of(), squadQuality, opponentQuality);
    }

    public TacticalProfile selectTactics(Team team,
                                         List<SquadPlayer> availablePlayers,
                                         int squadQuality,
                                         int opponentQuality) {
        TacticalProfile profile = tacticalProfileService.getOrCreateProfile(team);
        int difference = squadQuality - opponentQuality;
        if (difference >= 5) {
            profile.setBuildUpStyle(BuildUpStyle.SLOW_POSSESSION);
            profile.setChanceCreation(ChanceCreation.POSSESSION);
            profile.setAttackWidth(70);
            profile.setAttackingWidth(70);
            profile.setPressingIntensity(72);
            profile.setDefensiveLine(65);
            profile.setPassingRisk(62);
            profile.setCrossFrequency(62);
            profile.setCounterAttack(false);
            profile.setHighPress(true);
            profile.setOffsideTrap(true);
            profile.setTimeWasting(false);
        } else if (difference <= -5) {
            profile.setBuildUpStyle(BuildUpStyle.DIRECT);
            profile.setChanceCreation(ChanceCreation.FAST_ATTACK);
            profile.setAttackWidth(42);
            profile.setAttackingWidth(42);
            profile.setDefensiveWidth(38);
            profile.setDefensiveLine(35);
            profile.setPressingIntensity(38);
            profile.setPassingRisk(58);
            profile.setLongBallFrequency(68);
            profile.setCounterAttack(true);
            profile.setHighPress(false);
            profile.setOffsideTrap(false);
            profile.setTimeWasting(false);
        } else {
            profile.setBuildUpStyle(BuildUpStyle.BALANCED);
            profile.setChanceCreation(ChanceCreation.BALANCED);
            profile.setAttackWidth(50);
            profile.setAttackingWidth(50);
            profile.setDefensiveWidth(50);
            profile.setDefensiveLine(50);
            profile.setPressingIntensity(50);
            profile.setCrossFrequency(50);
            profile.setLongBallFrequency(50);
            profile.setPassingRisk(50);
            profile.setCounterAttack(false);
            profile.setHighPress(false);
            profile.setOffsideTrap(false);
            profile.setTimeWasting(false);
        }
        applySquadStrengths(profile, availablePlayers);
        return tacticalProfileService.saveProfile(profile);
    }

    public TacticalProfile adjustForMatchState(Team team, int goalDifference) {
        return adjustForMatchState(team, goalDifference, false, false, false, List.of());
    }

    public TacticalProfile adjustForMatchState(Team team,
                                               int goalDifference,
                                               boolean ownRedCard,
                                               boolean opponentRedCard,
                                               boolean extraTime,
                                               List<SquadPlayer> activePlayers) {
        TacticalProfile profile = tacticalProfileService.getOrCreateProfile(team);
        if (goalDifference < 0) {
            profile.setHighPress(true);
            profile.setPressingIntensity(Math.min(100, profile.getPressingIntensity() + 12));
            profile.setPassingRisk(Math.min(100, profile.getPassingRisk() + 10));
            profile.setChanceCreation(ChanceCreation.FAST_ATTACK);
        } else if (goalDifference > 0) {
            profile.setHighPress(false);
            profile.setPressingIntensity(Math.max(1, profile.getPressingIntensity() - 10));
            profile.setTimeWasting(true);
            profile.setPassingRisk(Math.max(1, profile.getPassingRisk() - 6));
            profile.setAttackWidth(Math.max(1, profile.getAttackWidth() - 5));
            profile.setAttackingWidth(Math.max(1, profile.getAttackingWidth() - 5));
        } else {
            profile.setTimeWasting(false);
        }

        if (ownRedCard) {
            profile.setHighPress(false);
            profile.setPressingIntensity(Math.max(1, profile.getPressingIntensity() - 18));
            profile.setDefensiveLine(Math.max(1, profile.getDefensiveLine() - 14));
            profile.setAttackWidth(Math.max(1, profile.getAttackWidth() - 8));
            profile.setAttackingWidth(Math.max(1, profile.getAttackingWidth() - 8));
            profile.setOffsideTrap(false);
        }
        if (opponentRedCard) {
            profile.setPressingIntensity(Math.min(100, profile.getPressingIntensity() + 8));
            profile.setAttackWidth(Math.min(100, profile.getAttackWidth() + 8));
            profile.setAttackingWidth(Math.min(100, profile.getAttackingWidth() + 8));
            profile.setPassingRisk(Math.min(100, profile.getPassingRisk() + 6));
        }
        if (extraTime && averageFitness(activePlayers) < 70) {
            profile.setHighPress(false);
            profile.setPressingIntensity(Math.max(1, profile.getPressingIntensity() - 12));
            profile.setPassingRisk(Math.max(1, profile.getPassingRisk() - 4));
        }
        return tacticalProfileService.saveProfile(profile);
    }

    private void applySquadStrengths(TacticalProfile profile,
                                     List<SquadPlayer> availablePlayers) {
        if (availablePlayers.isEmpty()) {
            return;
        }

        double attackerPace = averageAttribute(availablePlayers,
                List.of(PlayerPosition.RW, PlayerPosition.LW, PlayerPosition.ST),
                Attribute.PACE);
        double midfieldCreativity = averageAttribute(availablePlayers,
                List.of(PlayerPosition.CDM, PlayerPosition.CM, PlayerPosition.CAM),
                Attribute.CREATIVITY);
        double defensiveStrength = averageAttribute(availablePlayers,
                List.of(PlayerPosition.RB, PlayerPosition.CB, PlayerPosition.LB),
                Attribute.DEFENSE);
        double stamina = averageFitness(availablePlayers);

        if (attackerPace >= 84 && attackerPace > midfieldCreativity) {
            profile.setBuildUpStyle(BuildUpStyle.DIRECT);
            profile.setChanceCreation(ChanceCreation.FAST_ATTACK);
            profile.setCounterAttack(true);
            profile.setLongBallFrequency(Math.max(profile.getLongBallFrequency(), 60));
        }
        if (midfieldCreativity >= 82 && midfieldCreativity >= attackerPace) {
            profile.setBuildUpStyle(BuildUpStyle.SLOW_POSSESSION);
            profile.setChanceCreation(ChanceCreation.POSSESSION);
            profile.setPassingRisk(Math.max(45, Math.min(profile.getPassingRisk(), 62)));
        }
        if (defensiveStrength >= 82) {
            profile.setDefensiveLine(Math.max(profile.getDefensiveLine(), 58));
            profile.setOffsideTrap(true);
        } else if (defensiveStrength > 0 && defensiveStrength < 74) {
            profile.setDefensiveLine(Math.min(profile.getDefensiveLine(), 38));
            profile.setOffsideTrap(false);
        }
        if (stamina >= 78) {
            profile.setHighPress(true);
            profile.setPressingIntensity(Math.max(profile.getPressingIntensity(), 65));
        } else if (stamina > 0 && stamina < 68) {
            profile.setHighPress(false);
            profile.setPressingIntensity(Math.min(profile.getPressingIntensity(), 48));
        }
    }

    private double averageAttribute(List<SquadPlayer> players,
                                    List<PlayerPosition> positions,
                                    Attribute attribute) {
        return players.stream()
                .filter(player -> positions.contains(player.getPlayer().getPosition()))
                .mapToDouble(player -> switch (attribute) {
                    case PACE -> player.getPlayer().getPace();
                    case CREATIVITY -> (player.getPlayer().getPassing()
                            + player.getPlayer().getDribbling()) / 2.0;
                    case DEFENSE -> (player.getPlayer().getDefending()
                            + player.getPlayer().getPhysical()) / 2.0;
                })
                .average()
                .orElse(0);
    }

    private double averageFitness(List<SquadPlayer> players) {
        return players.stream()
                .map(player -> playerStateService.getOrCreateState(player.getPlayer()))
                .mapToDouble(PlayerState::getFitness)
                .average()
                .orElse(0);
    }

    private enum Attribute {
        PACE,
        CREATIVITY,
        DEFENSE
    }
}
