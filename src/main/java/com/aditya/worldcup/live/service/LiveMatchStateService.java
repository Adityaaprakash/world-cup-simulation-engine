package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEvent;
import com.aditya.worldcup.live.dto.LiveMatchEventType;
import com.aditya.worldcup.live.dto.LiveMatchPhase;
import com.aditya.worldcup.live.dto.LiveMatchSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Maintains in-memory, backend-authoritative live state for all currently active matches.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Sequence-monotonic: events with a sequence number ≤ the current latest are silently
 *       discarded (deduplication + out-of-order protection).</li>
 *   <li>Per-match locking ensures thread-safety without a single global lock.</li>
 *   <li>State is initialised on MATCH_STARTED and removed after FULL_TIME / ERROR so that
 *       completed snapshots remain available for late readers until explicitly expired.</li>
 *   <li>No simulation logic lives here — state is derived solely from canonical
 *       {@link LiveMatchEvent}s emitted by the Phase 12B broadcaster pipeline.</li>
 * </ul>
 */
@Service
@Slf4j
public class LiveMatchStateService {

    /** Internal mutable state bucket for a single active match. */
    private static final class MatchState {
        final ReentrantLock lock = new ReentrantLock();

        Long tournamentId;
        String homeTeamName;
        String awayTeamName;

        volatile int homeScore = 0;
        volatile int awayScore = 0;
        volatile int currentMinute = 0;
        volatile LiveMatchPhase phase = LiveMatchPhase.PRE_MATCH;
        volatile int latestSequence = 0;
        volatile LiveMatchEventType latestEventType = null;
        volatile Instant lastUpdated = Instant.now();
        final java.util.Deque<LiveMatchEvent> commentaryHistory = new java.util.concurrent.ConcurrentLinkedDeque<>();
    }

    // Match ID → live state
    private final ConcurrentHashMap<Long, MatchState> activeStates = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Returns a point-in-time snapshot for the given match, or
     * {@link Optional#empty()} if the match is not (or no longer) tracked.
     */
    public Optional<LiveMatchSnapshot> getSnapshot(Long matchId) {
        MatchState state = activeStates.get(matchId);
        if (state == null) {
            return Optional.empty();
        }
        return Optional.of(toSnapshot(matchId, state));
    }

    /**
     * Returns {@code true} if there is live state being tracked for {@code matchId}.
     */
    public boolean isLive(Long matchId) {
        return activeStates.containsKey(matchId);
    }

    /**
     * Apply a canonical {@link LiveMatchEvent} produced by the Phase 12B broadcaster.
     * The call is idempotent for duplicate sequence numbers and order-safe for
     * out-of-order delivery.
     */
    public void applyEvent(LiveMatchEvent event) {
        if (event == null) {
            return;
        }

        Long matchId = event.matchId();

        // Initialise state bucket on first event for a match
        if (event.eventType() == LiveMatchEventType.MATCH_STARTED) {
            MatchState fresh = new MatchState();
            fresh.tournamentId = event.tournamentId();
            // Names arrive in payload; best-effort extraction done later via FULL_TIME
            activeStates.putIfAbsent(matchId, fresh);
            log.debug("Initialised live state for match {}", matchId);
        }

        MatchState state = activeStates.get(matchId);
        if (state == null) {
            // Late arrival for a match we never saw MATCH_STARTED for — bootstrap defensively
            state = new MatchState();
            state.tournamentId = event.tournamentId();
            activeStates.putIfAbsent(matchId, state);
            state = activeStates.get(matchId);
        }

        state.lock.lock();
        try {
            // Sequence deduplication + out-of-order guard
            if (event.sequenceNumber() <= state.latestSequence
                    && event.eventType() != LiveMatchEventType.MATCH_STARTED) {
                log.trace("Discarding stale/duplicate event seq={} for match {} (current={})",
                        event.sequenceNumber(), matchId, state.latestSequence);
                return;
            }

            // Apply authoritative score from the event (broadcaster is the source of truth)
            if (event.homeScore() != null) {
                state.homeScore = event.homeScore();
            }
            if (event.awayScore() != null) {
                state.awayScore = event.awayScore();
            }

            // Update clock
            if (event.minute() != null) {
                state.currentMinute = event.minute();
            }

            // Derive phase from event type
            state.phase = derivePhase(event.eventType(), state.phase);

            state.latestSequence = event.sequenceNumber();
            state.latestEventType = event.eventType();
            state.lastUpdated = event.timestamp() != null ? event.timestamp() : Instant.now();

            // Extract team names from FULL_TIME payload if present
            if (event.eventType() == LiveMatchEventType.FULL_TIME && event.payload() != null) {
                Object finalResult = event.payload().get("finalResult");
                if (finalResult instanceof com.aditya.worldcup.simulation.dto.MatchSimulationResponse msr) {
                    if (state.homeTeamName == null) state.homeTeamName = msr.homeTeam();
                    if (state.awayTeamName == null) state.awayTeamName = msr.awayTeam();
                }
            }

            if (event.payload() != null && event.payload().containsKey("commentary")) {
                state.commentaryHistory.addLast(event);
                if (state.commentaryHistory.size() > 50) {
                    state.commentaryHistory.removeFirst();
                }
            }

            log.trace("Applied event seq={} type={} to match {}", event.sequenceNumber(), event.eventType(), matchId);

            // Do NOT remove state on FULL_TIME immediately — keep it available for late readers.
            // Clean-up is deferred; callers may invoke expireMatch() on their own schedule.

        } finally {
            state.lock.unlock();
        }
    }

    /**
     * Explicitly removes live state for a match (e.g. after a configurable retention window).
     */
    public void expireMatch(Long matchId) {
        if (activeStates.remove(matchId) != null) {
            log.info("Expired live state for match {}", matchId);
        }
    }

    /**
     * Seed team names when they become known (e.g. from the simulation response at kick-off).
     * This is optional — names are populated defensively from FULL_TIME payload if not provided earlier.
     */
    public void seedTeamNames(Long matchId, String homeTeamName, String awayTeamName) {
        MatchState state = activeStates.get(matchId);
        if (state == null) return;
        state.lock.lock();
        try {
            state.homeTeamName = homeTeamName;
            state.awayTeamName = awayTeamName;
        } finally {
            state.lock.unlock();
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private LiveMatchPhase derivePhase(LiveMatchEventType eventType, LiveMatchPhase current) {
        return switch (eventType) {
            case MATCH_STARTED -> LiveMatchPhase.PRE_MATCH;
            case KICK_OFF -> LiveMatchPhase.FIRST_HALF;
            case HALF_TIME -> LiveMatchPhase.HALF_TIME;
            case SECOND_HALF_STARTED -> LiveMatchPhase.SECOND_HALF;
            case EXTRA_TIME_STARTED -> LiveMatchPhase.EXTRA_TIME;
            case PENALTY_SHOOTOUT_STARTED -> LiveMatchPhase.PENALTY_SHOOTOUT;
            case FULL_TIME -> LiveMatchPhase.FULL_TIME;
            default -> current; // keep existing phase for MINUTE_UPDATE, GOAL, etc.
        };
    }

    private LiveMatchSnapshot toSnapshot(Long matchId, MatchState state) {
        return new LiveMatchSnapshot(
                matchId,
                state.tournamentId,
                state.homeTeamName,
                state.awayTeamName,
                state.homeScore,
                state.awayScore,
                state.currentMinute,
                state.phase,
                state.latestSequence,
                state.latestEventType,
                state.lastUpdated,
                new java.util.ArrayList<>(state.commentaryHistory)
        );
    }
}
