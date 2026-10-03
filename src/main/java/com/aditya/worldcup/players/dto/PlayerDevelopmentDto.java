package com.aditya.worldcup.players.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Phase 14C — Read-only player development data DTO.
 * Returned by GET /api/players/{id}/development.
 */
@Getter
@Builder
public class PlayerDevelopmentDto {

    /** Long-term development rating in range [-5, 10]. */
    private final Integer developmentRating;

    /** Accumulated progression points toward next development rating change. */
    private final Integer progressionTracker;

    /** Player's maximum potential overall rating. */
    private final Integer potential;

    /** Player's current age (drives age-curve behaviour). */
    private final Integer age;

    /** Human-readable development stage derived from age. */
    private final String developmentStage;

    /** Current overall rating. */
    private final Integer overallRating;

    /** Current training fatigue [0, 100]. */
    private final Integer fatigue;

    // recentAttributeChanges: NOT IMPLEMENTED — optional
}
