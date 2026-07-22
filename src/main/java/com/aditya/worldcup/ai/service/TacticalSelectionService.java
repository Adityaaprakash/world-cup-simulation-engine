package com.aditya.worldcup.ai.service;

import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.ChanceCreation;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.service.TacticalProfileService;
import com.aditya.worldcup.teams.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TacticalSelectionService {

    private final TacticalProfileService tacticalProfileService;

    public TacticalProfile selectTactics(Team team, int squadQuality,
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
        return tacticalProfileService.saveProfile(profile);
    }

    public TacticalProfile adjustForMatchState(Team team, int goalDifference) {
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
        }
        return tacticalProfileService.saveProfile(profile);
    }
}
