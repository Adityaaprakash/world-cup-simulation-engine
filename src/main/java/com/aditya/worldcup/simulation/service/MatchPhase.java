package com.aditya.worldcup.simulation.service;

public enum MatchPhase {
    OPENING,
    SETTLED,
    END_FIRST_HALF,
    RESTART,
    SUBSTITUTION_PHASE,
    CLOSING,
    EXTRA_TIME,
    PENALTY_SHOOTOUT;

    public static MatchPhase fromMinute(int minute, boolean extraTime) {
        if (minute > 90 || extraTime) {
            return EXTRA_TIME;
        }
        if (minute <= 15) {
            return OPENING;
        }
        if (minute <= 30) {
            return SETTLED;
        }
        if (minute <= 45) {
            return END_FIRST_HALF;
        }
        if (minute <= 60) {
            return RESTART;
        }
        if (minute <= 75) {
            return SUBSTITUTION_PHASE;
        }
        return CLOSING;
    }
}
