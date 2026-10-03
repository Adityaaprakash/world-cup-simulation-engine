package com.aditya.worldcup.training.service;

import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.players.entity.PlayerPosition;
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
     * {@link Player} entity.
     *
     * <ul>
     *   <li>GK → defending, physical</li>
     *   <li>RB/CB/LB → defending, physical</li>
     *   <li>CDM → defending, passing</li>
     *   <li>CM → passing, dribbling</li>
     *   <li>CAM → passing, dribbling, shooting</li>
     *   <li>RW/LW → pace, dribbling</li>
     *   <li>ST → shooting, pace</li>
     * </ul>
     *
     * @param player    the player whose attributes will be modified
     * @param intensity training intensity (controls magnitude of increase)
     * @return true if any attribute was changed, false if all attributes were already at ceiling
     */
    public static boolean applyPositionAttributeBoost(Player player, TrainingIntensity intensity) {
        int increment = switch (intensity) {
            case LIGHT -> 1;
            case NORMAL -> 1;
            case INTENSE -> 2;
        };

        boolean changed = false;

        switch (player.getPosition()) {
            case GK -> {
                changed |= boostDefending(player, increment);
                changed |= boostPhysical(player, increment);
            }
            case RB, CB, LB -> {
                changed |= boostDefending(player, increment);
                changed |= boostPhysical(player, increment);
            }
            case CDM -> {
                changed |= boostDefending(player, increment);
                changed |= boostPassing(player, increment);
            }
            case CM -> {
                changed |= boostPassing(player, increment);
                changed |= boostDribbling(player, increment);
            }
            case CAM -> {
                changed |= boostPassing(player, increment);
                changed |= boostDribbling(player, increment);
                changed |= boostShooting(player, increment);
            }
            case RW, LW -> {
                changed |= boostPace(player, increment);
                changed |= boostDribbling(player, increment);
            }
            case ST -> {
                changed |= boostShooting(player, increment);
                changed |= boostPace(player, increment);
            }
        }

        if (changed) {
            recalculateOverall(player);
        }
        return changed;
    }

    // ------------------------------------------------------------------ //
    // Private attribute mutators — each clamps to [MIN_ATTRIBUTE, MAX_ATTRIBUTE]
    // ------------------------------------------------------------------ //

    private static boolean boostPace(Player player, int increment) {
        int current = player.getPace();
        int updated = Math.min(MAX_ATTRIBUTE, current + increment);
        if (updated == current) return false;
        player.setPace(updated);
        return true;
    }

    private static boolean boostShooting(Player player, int increment) {
        int current = player.getShooting();
        int updated = Math.min(MAX_ATTRIBUTE, current + increment);
        if (updated == current) return false;
        player.setShooting(updated);
        return true;
    }

    private static boolean boostPassing(Player player, int increment) {
        int current = player.getPassing();
        int updated = Math.min(MAX_ATTRIBUTE, current + increment);
        if (updated == current) return false;
        player.setPassing(updated);
        return true;
    }

    private static boolean boostDribbling(Player player, int increment) {
        int current = player.getDribbling();
        int updated = Math.min(MAX_ATTRIBUTE, current + increment);
        if (updated == current) return false;
        player.setDribbling(updated);
        return true;
    }

    private static boolean boostDefending(Player player, int increment) {
        int current = player.getDefending();
        int updated = Math.min(MAX_ATTRIBUTE, current + increment);
        if (updated == current) return false;
        player.setDefending(updated);
        return true;
    }

    private static boolean boostPhysical(Player player, int increment) {
        int current = player.getPhysical();
        int updated = Math.min(MAX_ATTRIBUTE, current + increment);
        if (updated == current) return false;
        player.setPhysical(updated);
        return true;
    }

    /**
     * Recalculates overallRating as the integer average of the six skill attributes,
     * clamped to [MIN_ATTRIBUTE, MAX_ATTRIBUTE].
     *
     * This mirrors the way attributes already contribute to overall rating
     * (as used by the existing simulation engine for team strength calculations).
     */
    static void recalculateOverall(Player player) {
        int avg = (player.getPace()
                + player.getShooting()
                + player.getPassing()
                + player.getDribbling()
                + player.getDefending()
                + player.getPhysical()) / 6;
        player.setOverallRating(Math.min(MAX_ATTRIBUTE, Math.max(MIN_ATTRIBUTE, avg)));
    }
}
