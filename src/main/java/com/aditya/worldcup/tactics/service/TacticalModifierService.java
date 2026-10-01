package com.aditya.worldcup.tactics.service;

import com.aditya.worldcup.tactics.entity.AttackingApproach;
import com.aditya.worldcup.tactics.entity.BuildUpStyle;
import com.aditya.worldcup.tactics.entity.DefensiveBlock;
import com.aditya.worldcup.tactics.entity.DefensiveLine;
import com.aditya.worldcup.tactics.entity.PassingStyle;
import com.aditya.worldcup.tactics.entity.PressingIntensity;
import com.aditya.worldcup.tactics.entity.Tempo;
import com.aditya.worldcup.tactics.entity.Width;
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
                centredWidth(profile.getWidth()) * 0.25,
                calculateOffsideModifier(profile)
        );
    }

    public double calculatePossessionModifier(TacticalProfile profile) {
        double style = profile.getBuildUpStyle() == BuildUpStyle.POSSESSION ? 0.7
                : profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? -0.55 : 0;
        double chance = profile.getAttackingApproach() == AttackingApproach.CONSERVATIVE ? 0.3
                : profile.getAttackingApproach() == AttackingApproach.ATTACKING ? -0.2 : 0;
        return style + chance - centredPassing(profile.getPassingStyle()) * 0.2;
    }

    public double calculateAttackModifier(TacticalProfile profile) {
        double style = profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? 0.35 : 0;
        double chance = profile.getAttackingApproach() == AttackingApproach.ATTACKING ? 0.55
                : profile.getAttackingApproach() == AttackingApproach.CONSERVATIVE ? -0.15 : 0;
        return style + chance + centredPassing(profile.getPassingStyle()) * 0.35
                + centredTempo(profile.getTempo()) * 0.15
                + centredWidth(profile.getWidth()) * 0.2
                + calculatePressModifier(profile) * 0.2
                - (profile.getTempo() == Tempo.SLOW ? 0.25 : 0);
    }

    public double calculateDefenseModifier(TacticalProfile profile) {
        return centredWidth(profile.getWidth()) * -0.25
                + centredDefensiveLine(profile.getDefensiveLine()) * 0.15
                + (profile.getDefensiveLine() == DefensiveLine.HIGH ? 0.25 : 0);
    }

    public double calculateCounterModifier(TacticalProfile profile,
                                           TacticalProfile opponent) {
        double opponentHighLine = Math.max(0, centredDefensiveLine(opponent.getDefensiveLine()));
        return (profile.getDefensiveBlock() == DefensiveBlock.LOW_BLOCK ? 0.7 : 0)
                + (profile.getAttackingApproach() == AttackingApproach.ATTACKING ? 0.3 : 0)
                + opponentHighLine * 0.55;
    }

    public double calculatePressModifier(TacticalProfile profile) {
        double press = profile.getPressingIntensity() == PressingIntensity.HIGH ? 1.0 : profile.getPressingIntensity() == PressingIntensity.LOW ? -1.0 : 0.0;
        return press
                + centredDefensiveLine(profile.getDefensiveLine()) * 0.2
                + (profile.getDefensiveBlock() == DefensiveBlock.HIGH_BLOCK ? 0.55 : 0);
    }

    public double calculateFatigueModifier(TacticalProfile profile) {
        double press = profile.getPressingIntensity() == PressingIntensity.HIGH ? 1.0 : profile.getPressingIntensity() == PressingIntensity.LOW ? -1.0 : 0.0;
        return Math.max(0, press) * 0.6
                + (profile.getDefensiveBlock() == DefensiveBlock.HIGH_BLOCK ? 0.45 : 0)
                + (profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? 0.1 : 0);
    }

    public double calculateDisciplineModifier(TacticalProfile profile) {
        return Math.max(0, calculatePressModifier(profile)) * 0.45
                + centredDefensiveLine(profile.getDefensiveLine()) * 0.1;
    }

    private double calculatePassingModifier(TacticalProfile profile) {
        double style = profile.getBuildUpStyle() == BuildUpStyle.POSSESSION ? 0.45
                : profile.getBuildUpStyle() == BuildUpStyle.DIRECT ? -0.35 : 0;
        return style - centredPassing(profile.getPassingStyle()) * 0.45;
    }

    private double calculateOffsideModifier(TacticalProfile profile) {
        double val = (profile.getDefensiveLine() == DefensiveLine.HIGH ? 0.55 : 0)
                + Math.max(0, centredDefensiveLine(profile.getDefensiveLine())) * 0.35;
        return val;
    }

    private double centredDefensiveLine(DefensiveLine line) {
        return line == DefensiveLine.HIGH ? 1.0 : line == DefensiveLine.DEEP ? -1.0 : 0.0;
    }
    private double centredWidth(Width width) {
        return width == Width.WIDE ? 1.0 : width == Width.NARROW ? -1.0 : 0.0;
    }
    private double centredPassing(PassingStyle style) {
        return style == PassingStyle.DIRECT ? 1.0 : style == PassingStyle.SHORT ? -1.0 : 0.0;
    }
    private double centredTempo(Tempo tempo) {
        return tempo == Tempo.FAST ? 1.0 : tempo == Tempo.SLOW ? -1.0 : 0.0;
    }
}
