package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.ChanceCreation;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TacticalModifierService {

    public TacticalMatchModifiers calculateModifiers(TacticalProfile profile,
                                                      TacticalProfile opponent) {
        return new TacticalMatchModifiers(
                calculatePossessionModifier(profile),
                calculateAttackModifier(profile),
                calculateDefenseModifier(profile),
                calculateCounterModifier(profile, opponent),
                calculatePressModifier(profile),
                calculateFatigueModifier(profile),
                calculateDisciplineModifier(profile),
                calculatePassingModifier(profile),
                centred(profile.getCrossFrequency()) + centred(profile.getAttackingWidth()) * 0.25
                        + centred(profile.getAttackWidth()) * 0.15,
                calculateOffsideModifier(profile)
        );
    }

    public double calculatePossessionModifier(TacticalProfile profile) {
        double style = profile.getBuildUpStyle() == BuildUpStyle.SLOW_POSSESSION ? 0.7
                : profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? -0.55 : 0;
        double chance = profile.getChanceCreation() == ChanceCreation.POSSESSION ? 0.3
                : profile.getChanceCreation() == ChanceCreation.FAST_ATTACK ? -0.2 : 0;
        return style + chance - centred(profile.getPassingRisk()) * 0.2;
    }

    public double calculateAttackModifier(TacticalProfile profile) {
        double style = profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? 0.35 : 0;
        double chance = profile.getChanceCreation() == ChanceCreation.FAST_ATTACK ? 0.55
                : profile.getChanceCreation() == ChanceCreation.POSSESSION ? -0.15 : 0;
        return style + chance + centred(profile.getPassingRisk()) * 0.35
                + centred(profile.getLongBallFrequency()) * 0.15
                + centred(profile.getAttackWidth()) * 0.2
                + calculatePressModifier(profile) * 0.2
                - (Boolean.TRUE.equals(profile.getTimeWasting()) ? 0.25 : 0);
    }

    public double calculateDefenseModifier(TacticalProfile profile) {
        return centred(profile.getDefensiveWidth()) * -0.25
                + centred(profile.getDefensiveLine()) * 0.15
                + (Boolean.TRUE.equals(profile.getOffsideTrap()) ? 0.25 : 0);
    }

    public double calculateCounterModifier(TacticalProfile profile,
                                           TacticalProfile opponent) {
        double opponentHighLine = Math.max(0, centred(opponent.getDefensiveLine()));
        return (Boolean.TRUE.equals(profile.getCounterAttack()) ? 0.7 : 0)
                + (profile.getChanceCreation() == ChanceCreation.FAST_ATTACK ? 0.3 : 0)
                + opponentHighLine * 0.55;
    }

    public double calculatePressModifier(TacticalProfile profile) {
        return centred(profile.getPressingIntensity())
                + centred(profile.getDefensiveLine()) * 0.2
                + (Boolean.TRUE.equals(profile.getHighPress()) ? 0.55 : 0);
    }

    public double calculateFatigueModifier(TacticalProfile profile) {
        return Math.max(0, centred(profile.getPressingIntensity())) * 0.6
                + (Boolean.TRUE.equals(profile.getHighPress()) ? 0.45 : 0)
                + (profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? 0.1 : 0);
    }

    public double calculateDisciplineModifier(TacticalProfile profile) {
        return Math.max(0, calculatePressModifier(profile)) * 0.45
                + centred(profile.getDefensiveLine()) * 0.1;
    }

    private double calculatePassingModifier(TacticalProfile profile) {
        double style = profile.getBuildUpStyle() == BuildUpStyle.SLOW_POSSESSION ? 0.45
                : profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? -0.35 : 0;
        return style - centred(profile.getPassingRisk()) * 0.45;
    }

    private double calculateOffsideModifier(TacticalProfile profile) {
        return (Boolean.TRUE.equals(profile.getOffsideTrap()) ? 0.55 : 0)
                + Math.max(0, centred(profile.getDefensiveLine())) * 0.35;
    }

    private double centred(Integer value) {
        return (value - 50) / 50.0;
    }
}
