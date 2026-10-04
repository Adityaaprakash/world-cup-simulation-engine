package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.players.service.PlayerStateService;
import com.aditya.worldcup.squadplayers.entity.SquadPlayer;
import com.aditya.worldcup.tactics.entity.AttackingApproach;
import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.DefensiveBlock;
import com.aditya.worldcup.tactics.entity.DefensiveLine;
import com.aditya.worldcup.tactics.entity.PassingStyle;
import com.aditya.worldcup.tactics.entity.PressingIntensity;
import com.aditya.worldcup.tactics.entity.Tempo;
import com.aditya.worldcup.tactics.entity.Width;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.service.TacticalProfileService;
import com.aditya.worldcup.tactics.dto.MatchPlanDto;
import com.aditya.worldcup.tactics.entity.TacticalApproach;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.squads.entity.Squad;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService;
import com.aditya.worldcup.players.service.PlayerEffectiveRatingService.EffectiveAttributes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TacticalSelectionService {

    private final TacticalProfileService tacticalProfileService;
    private final PlayerStateService playerStateService;
    private final PlayerEffectiveRatingService playerEffectiveRatingService;

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
            profile.setBuildUpStyle(BuildUpStyle.POSSESSION);
            profile.setAttackingApproach(AttackingApproach.ATTACKING);
            profile.setWidth(Width.WIDE);
            profile.setPressingIntensity(PressingIntensity.HIGH);
            profile.setDefensiveLine(DefensiveLine.HIGH);
            profile.setPassingStyle(PassingStyle.SHORT);
            profile.setTempo(Tempo.SLOW);
            profile.setDefensiveBlock(DefensiveBlock.HIGH_BLOCK);
        } else if (difference <= -5) {
            profile.setBuildUpStyle(BuildUpStyle.DIRECT);
            profile.setAttackingApproach(AttackingApproach.CONSERVATIVE);
            profile.setWidth(Width.NARROW);
            profile.setDefensiveLine(DefensiveLine.DEEP);
            profile.setPressingIntensity(PressingIntensity.LOW);
            profile.setPassingStyle(PassingStyle.DIRECT);
            profile.setTempo(Tempo.FAST);
            profile.setDefensiveBlock(DefensiveBlock.LOW_BLOCK);
        } else {
            profile.setBuildUpStyle(BuildUpStyle.BALANCED);
            profile.setAttackingApproach(AttackingApproach.BALANCED);
            profile.setWidth(Width.BALANCED);
            profile.setDefensiveLine(DefensiveLine.BALANCED);
            profile.setPressingIntensity(PressingIntensity.BALANCED);
            profile.setPassingStyle(PassingStyle.MIXED);
            profile.setTempo(Tempo.BALANCED);
            profile.setDefensiveBlock(DefensiveBlock.MID_BLOCK);
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
            profile.setPressingIntensity(PressingIntensity.HIGH);
            profile.setPassingStyle(PassingStyle.DIRECT);
            profile.setAttackingApproach(AttackingApproach.ATTACKING);
        } else if (goalDifference > 0) {
            profile.setPressingIntensity(PressingIntensity.LOW);
            profile.setTempo(Tempo.SLOW);
            profile.setPassingStyle(PassingStyle.SHORT);
            profile.setWidth(Width.NARROW);
        }

        if (ownRedCard) {
            profile.setPressingIntensity(PressingIntensity.LOW);
            profile.setDefensiveLine(DefensiveLine.DEEP);
            profile.setWidth(Width.NARROW);
            profile.setDefensiveBlock(DefensiveBlock.LOW_BLOCK);
        }
        if (opponentRedCard) {
            profile.setPressingIntensity(PressingIntensity.HIGH);
            profile.setWidth(Width.WIDE);
            profile.setPassingStyle(PassingStyle.DIRECT);
        }
        if (extraTime && averageFitness(activePlayers) < 70) {
            profile.setPressingIntensity(PressingIntensity.LOW);
            profile.setTempo(Tempo.SLOW);
        }
        return tacticalProfileService.saveProfile(profile);
    }

    public MatchPlanDto generateMatchPlan(Squad squad, Squad opponent, TacticalProfile profile, Long matchId) {
        TacticalApproach approach = TacticalApproach.BALANCED;

        int diff = squad.getTeam().getOverallRating() - opponent.getTeam().getOverallRating();
        if (diff > 5) {
            approach = TacticalApproach.ATTACKING;
        } else if (diff < -5) {
            approach = TacticalApproach.DEFENSIVE;
        } else {
            if (profile.getPressingIntensity() == PressingIntensity.HIGH) {
                approach = TacticalApproach.HIGH_PRESS;
            } else if (profile.getBuildUpStyle() == BuildUpStyle.POSSESSION) {
                approach = TacticalApproach.POSSESSION;
            } else if (profile.getTempo() == Tempo.FAST && profile.getDefensiveBlock() == DefensiveBlock.LOW_BLOCK) {
                approach = TacticalApproach.COUNTER_ATTACK;
            }
        }

        return MatchPlanDto.builder()
                .matchId(matchId)
                .squadId(squad.getId())
                .tacticalApproach(approach)
                .pressingIntensity(profile.getPressingIntensity() == PressingIntensity.HIGH ? 80 : profile.getPressingIntensity() == PressingIntensity.BALANCED ? 50 : 20)
                .tempo(profile.getTempo() == Tempo.FAST ? 80 : profile.getTempo() == Tempo.BALANCED ? 50 : 20)
                .defensiveLine(profile.getDefensiveLine() == DefensiveLine.HIGH ? 80 : profile.getDefensiveLine() == DefensiveLine.BALANCED ? 50 : 20)
                .attackingWidth(profile.getWidth() == Width.WIDE ? 80 : profile.getWidth() == Width.BALANCED ? 50 : 20)
                .counterAttack(profile.getDefensiveBlock() == DefensiveBlock.LOW_BLOCK && profile.getTempo() == Tempo.FAST)
                .offsideTrap(profile.getDefensiveLine() == DefensiveLine.HIGH)
                .build();
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
            profile.setPassingStyle(PassingStyle.DIRECT);
            profile.setTempo(Tempo.FAST);
        }
        if (midfieldCreativity >= 82 && midfieldCreativity >= attackerPace) {
            profile.setBuildUpStyle(BuildUpStyle.POSSESSION);
            profile.setPassingStyle(PassingStyle.SHORT);
            profile.setTempo(Tempo.SLOW);
        }
        if (defensiveStrength >= 82) {
            profile.setDefensiveLine(DefensiveLine.HIGH);
        } else if (defensiveStrength > 0 && defensiveStrength < 74) {
            profile.setDefensiveLine(DefensiveLine.DEEP);
        }
        if (stamina >= 78) {
            profile.setPressingIntensity(PressingIntensity.HIGH);
        } else if (stamina > 0 && stamina < 68) {
            profile.setPressingIntensity(PressingIntensity.LOW);
        }
    }

    private double averageAttribute(List<SquadPlayer> players,
                                    List<PlayerPosition> positions,
                                    Attribute attribute) {
        return players.stream()
                .filter(player -> positions.contains(player.getPlayer().getPosition()))
                .map(player -> playerEffectiveRatingService.getEffectiveAttributes(
                        player.getPlayer(),
                        playerStateService.getOrCreateState(player.getPlayer())))
                .mapToDouble(attrs -> switch (attribute) {
                    case PACE -> attrs.pace();
                    case CREATIVITY -> (attrs.passing()
                            + attrs.dribbling()) / 2.0;
                    case DEFENSE -> (attrs.defending()
                            + attrs.physical()) / 2.0;
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
