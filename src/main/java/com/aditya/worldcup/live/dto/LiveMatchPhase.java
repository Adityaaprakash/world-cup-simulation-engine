package com.aditya.worldcup.live.dto;

/**
 * Represents the current phase of a live match.
 * Maps cleanly onto the Phase 12A/12B event lifecycle.
 */
public enum LiveMatchPhase {
    PRE_MATCH,
    FIRST_HALF,
    HALF_TIME,
    SECOND_HALF,
    EXTRA_TIME,
    PENALTY_SHOOTOUT,
    FULL_TIME
}
