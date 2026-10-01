package com.aditya.worldcup.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated tournament-level live state, derived from per-match {@link LiveMatchSnapshot}s.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Each entry is sourced directly from the existing match-level live architecture.</li>
 *   <li>No global tournament sequence number — sequences remain match-scoped.</li>
 *   <li>Match entries are classified into active / recently-completed / upcoming at
 *       query time; no secondary state machine is maintained here.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TournamentLiveState(
        Long tournamentId,
        /** Matches that are currently in progress (any non-PRE_MATCH / non-FULL_TIME phase). */
        List<MatchLiveEntry> activeMatches,
        /** Matches that have reached FULL_TIME during this live session. */
        List<MatchLiveEntry> recentlyCompletedMatches,
        /** Matches not yet started (phase == PRE_MATCH or no live state at all). */
        List<MatchLiveEntry> upcomingMatches,
        Instant updatedAt
) {

    /**
     * A single match summary within the tournament live view.
     * Deliberately avoids duplicating all {@link LiveMatchSnapshot} fields —
     * the full snapshot remains available via {@code GET /api/matches/{id}/live}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MatchLiveEntry(
            Long matchId,
            String homeTeam,
            String awayTeam,
            int homeScore,
            int awayScore,
            Integer minute,
            Integer addedTime,
            LiveMatchPhase phase,
            /** The most recent event type for headline display. */
            LiveMatchEventType latestEventType,
            /** Canonical sequence number of the latest event for client-side deduplication. */
            int latestSequence,
            /**
             * Latest commentary text, extracted from the snapshot's commentary history.
             * Null if no commentary is available.
             */
            String latestCommentary
    ) {
    }
}
