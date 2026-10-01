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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TacticalModifierServiceTest {

    private TacticalModifierService service;

    @BeforeEach
    void setUp() {
        service = new TacticalModifierService();
    }

    @Test
    void testCalculatePossessionModifier() {
        TacticalProfile profile = new TacticalProfile();
        profile.setBuildUpStyle(BuildUpStyle.POSSESSION);
        profile.setAttackingApproach(AttackingApproach.CONSERVATIVE);
        profile.setPassingStyle(PassingStyle.SHORT);

        double modifier = service.calculatePossessionModifier(profile);
        assertEquals(1.2, modifier, 0.001); // 0.7 + 0.3 - (-1.0 * 0.2) = 1.2
    }

    @Test
    void testCalculateAttackModifier() {
        TacticalProfile profile = new TacticalProfile();
        profile.setBuildUpStyle(BuildUpStyle.DIRECT);
        profile.setAttackingApproach(AttackingApproach.ATTACKING);
        profile.setPassingStyle(PassingStyle.DIRECT);
        profile.setTempo(Tempo.FAST);
        profile.setWidth(Width.WIDE);
        profile.setPressingIntensity(PressingIntensity.HIGH);
        profile.setDefensiveLine(DefensiveLine.HIGH);
        profile.setDefensiveBlock(DefensiveBlock.HIGH_BLOCK);

        double modifier = service.calculateAttackModifier(profile);
        assertTrue(modifier > 1.0); // 0.35 + 0.55 + 0.35 + 0.15 + 0.2 + (press 1.75 * 0.2) - 0
    }

    @Test
    void testCalculateCounterModifier() {
        TacticalProfile profile = new TacticalProfile();
        profile.setDefensiveBlock(DefensiveBlock.LOW_BLOCK);
        profile.setAttackingApproach(AttackingApproach.ATTACKING);

        TacticalProfile opponent = new TacticalProfile();
        opponent.setDefensiveLine(DefensiveLine.HIGH);

        double modifier = service.calculateCounterModifier(profile, opponent);
        assertEquals(1.55, modifier, 0.001); // 0.7 + 0.3 + 0.55
    }

    @Test
    void testCalculateModifiersReturnsValidMatchModifiers() {
        TacticalProfile profile = new TacticalProfile();
        profile.setBuildUpStyle(BuildUpStyle.BALANCED);
        profile.setAttackingApproach(AttackingApproach.BALANCED);
        profile.setPassingStyle(PassingStyle.MIXED);
        profile.setTempo(Tempo.BALANCED);
        profile.setWidth(Width.BALANCED);
        profile.setPressingIntensity(PressingIntensity.BALANCED);
        profile.setDefensiveLine(DefensiveLine.BALANCED);
        profile.setDefensiveBlock(DefensiveBlock.MID_BLOCK);

        TacticalProfile opponent = new TacticalProfile();
        opponent.setDefensiveLine(DefensiveLine.BALANCED);

        TacticalMatchModifiers modifiers = service.calculateModifiers(profile, opponent);
        assertEquals(0.0, modifiers.possessionModifier(), 0.001);
        assertEquals(0.0, modifiers.attackModifier(), 0.001);
        assertEquals(0.0, modifiers.defenseModifier(), 0.001);
        assertEquals(0.0, modifiers.counterModifier(), 0.001);
        assertEquals(0.0, modifiers.pressModifier(), 0.001);
    }
}
