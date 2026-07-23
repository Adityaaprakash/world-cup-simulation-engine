package com.aditya.worldcup.ai.service;

public enum MatchImportance {
    GROUP_STAGE(0.85, 1.15, false),
    KNOCKOUT(1.0, 0.95, true),
    SEMI_FINAL(1.1, 0.75, true),
    FINAL(1.2, 0.45, true);

    private final double qualityWeight;
    private final double rotationWeight;
    private final boolean extraTimePossible;

    MatchImportance(double qualityWeight,
                    double rotationWeight,
                    boolean extraTimePossible) {
        this.qualityWeight = qualityWeight;
        this.rotationWeight = rotationWeight;
        this.extraTimePossible = extraTimePossible;
    }

    public double qualityWeight() {
        return qualityWeight;
    }

    public double rotationWeight() {
        return rotationWeight;
    }

    public boolean extraTimePossible() {
        return extraTimePossible;
    }
}
