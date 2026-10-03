package com.aditya.worldcup.training.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
import com.aditya.worldcup.players.entity.PlayerState;
import com.aditya.worldcup.training.entity.TrainingIntensity;

/**
 * Phase 14C — Position Training Utility.
 *
 * Maps a player's {@link PlayerPosition} to the actual {@link Player} attribute fields
 * that should receive a direct attribute boost during POSITION training, and applies
 * those boosts in-place on the {@link Player} entity.
 *
 * <p>Only attributes that actually exist on the Player entity are mutated:
 * pace, shooting, passing, dribbling, defending, physical, overallRating.</p>
 *
 * <p>No primary position is changed. No new attributes are invented.</p>
 */
public final class PositionTrainingUtil {

    private static final int MAX_ATTRIBUTE = 99;
    private static final int MIN_ATTRIBUTE = 1;

    private PositionTrainingUtil() {}

    /**
     * Returns a generic intensity bonus (0–2) used to amplify the base
     * progression multiplier when category == POSITION.
     */
    public static int positionBonus(PlayerPosition position, TrainingIntensity intensity) {
        int base = switch (intensity) {
            case LIGHT -> 0;
            case NORMAL -> 1;
            case INTENSE -> 2;
        };
        return base;
    }

    /**
     * Applies a small, bounded, position-specific attribute boost directly to the
     * {@link PlayerState} delta fields to guarantee strict manager isolation.
     *
     * @param player    the immutable base player
     * @param state     the manager-specific state where deltas are persisted
     * @param intensity training intensity (controls magnitude of increase)
     * @return true if any attribute was changed, false if all attributes were already at ceiling
     */
    public static boolean applyPositionAttributeBoost(Player player, PlayerState state, TrainingIntensity intensity) {
        int increment = switch (intensity) {
            case LIGHT -> 1;
            case NORMAL -> 1;
            case INTENSE -> 2;
        };

        boolean changed = false;

        switch (player.getPosition()) {
            case GK -> {
                changed |= boostDefending(player, state, increment);
                changed |= boostPhysical(player, state, increment);
            }
            case RB, CB, LB -> {
                changed |= boostDefending(player, state, increment);
                changed |= boostPhysical(player, state, increment);
            }
            case CDM -> {
                changed |= boostDefending(player, state, increment);
                changed |= boostPassing(player, state, increment);
            }
            case CM -> {
                changed |= boostPassing(player, state, increment);
                changed |= boostDribbling(player, state, increment);
            }
            case CAM -> {
                changed |= boostPassing(player, state, increment);
                changed |= boostDribbling(player, state, increment);
                changed |= boostShooting(player, state, increment);
            }
            case RW, LW -> {
                changed |= boostPace(player, state, increment);
                changed |= boostDribbling(player, state, increment);
            }
            case ST -> {
                changed |= boostShooting(player, state, increment);
                changed |= boostPace(player, state, increment);
            }
        }

        if (changed) {
            recalculateOverallDelta(player, state);
        }
        return changed;
    }

    // ------------------------------------------------------------------ //
    // Private attribute mutators — each maintains the MAX_ATTRIBUTE ceiling 
    // against the COMBINED (base + delta) score.
    // ------------------------------------------------------------------ //

    private static boolean boostPace(Player player, PlayerState state, int increment) {
        int effective = player.getPace() + state.getPaceDelta();
        int maxAllowed = Math.min(MAX_ATTRIBUTE, player.getPotential());
        int updated = Math.min(maxAllowed, effective + increment);
        if (updated <= effective) return false;
        state.setPaceDelta(state.getPaceDelta() + (updated - effective));
        return true;
    }

    private static boolean boostShooting(Player player, PlayerState state, int increment) {
        int effective = player.getShooting() + state.getShootingDelta();
        int maxAllowed = Math.min(MAX_ATTRIBUTE, player.getPotential());
        int updated = Math.min(maxAllowed, effective + increment);
        if (updated <= effective) return false;
        state.setShootingDelta(state.getShootingDelta() + (updated - effective));
        return true;
    }

    private static boolean boostPassing(Player player, PlayerState state, int increment) {
        int effective = player.getPassing() + state.getPassingDelta();
        int maxAllowed = Math.min(MAX_ATTRIBUTE, player.getPotential());
        int updated = Math.min(maxAllowed, effective + increment);
        if (updated <= effective) return false;
        state.setPassingDelta(state.getPassingDelta() + (updated - effective));
        return true;
    }

    private static boolean boostDribbling(Player player, PlayerState state, int increment) {
        int effective = player.getDribbling() + state.getDribblingDelta();
        int maxAllowed = Math.min(MAX_ATTRIBUTE, player.getPotential());
        int updated = Math.min(maxAllowed, effective + increment);
        if (updated <= effective) return false;
        state.setDribblingDelta(state.getDribblingDelta() + (updated - effective));
        return true;
    }

    private static boolean boostDefending(Player player, PlayerState state, int increment) {
        int effective = player.getDefending() + state.getDefendingDelta();
        int maxAllowed = Math.min(MAX_ATTRIBUTE, player.getPotential());
        int updated = Math.min(maxAllowed, effective + increment);
        if (updated <= effective) return false;
        state.setDefendingDelta(state.getDefendingDelta() + (updated - effective));
        return true;
    }

    private static boolean boostPhysical(Player player, PlayerState state, int increment) {
        int effective = player.getPhysical() + state.getPhysicalDelta();
        int maxAllowed = Math.min(MAX_ATTRIBUTE, player.getPotential());
        int updated = Math.min(maxAllowed, effective + increment);
        if (updated <= effective) return false;
        state.setPhysicalDelta(state.getPhysicalDelta() + (updated - effective));
        return true;
    }

    /**
     * Recalculates developmentRating delta to correctly offset the average
     * of the isolated attribute deltas so that PlayerEffectiveRatingService 
     * correctly surfaces the newly bounded progression natively.
     */
    static void recalculateOverallDelta(Player player, PlayerState state) {
        int avgEffective = (
                (player.getPace() + state.getPaceDelta())
                + (player.getShooting() + state.getShootingDelta())
                + (player.getPassing() + state.getPassingDelta())
                + (player.getDribbling() + state.getDribblingDelta())
                + (player.getDefending() + state.getDefendingDelta())
                + (player.getPhysical() + state.getPhysicalDelta())) / 6;
                
        int baseAvg = (player.getPace() + player.getShooting() + player.getPassing()
                       + player.getDribbling() + player.getDefending() + player.getPhysical()) / 6;
                       
        int diff = avgEffective - baseAvg;
        
        // We set developmentRating to reflect the attribute growth! 
        // Note: the effective potential cap was already strictly enforced during attribute boost logic.
        state.setDevelopmentRating(Math.max(0, diff));
    }
}
