package com.aditya.worldcup.tactics.service;

public record TacticalMatchModifiers(
        double possessionModifier,
        double attackModifier,
        double defenseModifier,
        double counterModifier,
        double pressModifier,
        double fatigueModifier,
        double disciplineModifier,
        double passingModifier,
        double crossingModifier,
        double offsideModifier
) {

    public static TacticalMatchModifiers balanced() {
        return new TacticalMatchModifiers(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
