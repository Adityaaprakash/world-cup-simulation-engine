package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEvent;
import com.aditya.worldcup.live.dto.LiveMatchPhase;
import com.aditya.worldcup.live.dto.LiveMatchSnapshot;
import com.aditya.worldcup.live.dto.TournamentLiveState;
import com.aditya.worldcup.live.dto.TournamentLiveState.MatchLiveEntry;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aggregates per-match live snapshots into a tournament-level live view.
 *
 * <p>Architecture: the existing {@link LiveMatchStateService} remains the authoritative
 * source of truth for all per-match live state. This service is purely a read-time
 * aggregation layer — it does NOT maintain any independent event state or produce events.
 *
 * <p>No global tournament sequence number is introduced; sequences remain scoped to
 * individual matches (matchId + sequenceNumber) as per the Phase 12 canonical contract.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentLiveAggregator {

    private final LiveMatchStateService stateService;
    private final MatchRepository matchRepository;

    /**
     * Returns the current tournament-level live view by aggregating per-match snapshots.
     *
     * <p>Matches are classified at query time:
     * <ul>
     *   <li><b>active</b>: live state exists AND phase is not PRE_MATCH / FULL_TIME</li>
     *   <li><b>recentlyCompleted</b>: live state exists AND phase is FULL_TIME</li>
     *   <li><b>upcoming</b>: no live state, or live state in PRE_MATCH</li>
     * </ul>
     *
     * <p>Matches not in the DB for the given tournament are silently omitted.
     * A failed per-match lookup does not crash the entire tournament view.
     *
     * @param tournamentId the tournament to aggregate
     * @return the aggregated live state (never null; lists may be empty)
     */
    public TournamentLiveState aggregate(Long tournamentId) {
        List<Match> matches = matchRepository.findByTournamentIdOrderById(tournamentId);

        List<MatchLiveEntry> active = new ArrayList<>();
        List<MatchLiveEntry> recentlyCompleted = new ArrayList<>();
        List<MatchLiveEntry> upcoming = new ArrayList<>();

        for (Match match : matches) {
            try {
                MatchLiveEntry entry = buildEntry(match);
                classify(entry, match, active, recentlyCompleted, upcoming);
            } catch (Exception ex) {
                log.warn("Failed to build live entry for match {} in tournament {}: {}",
                        match.getId(), tournamentId, ex.getMessage());
            }
        }

        log.debug("Tournament {} live: {} active, {} completed, {} upcoming",
                tournamentId, active.size(), recentlyCompleted.size(), upcoming.size());

        return new TournamentLiveState(tournamentId, active, recentlyCompleted, upcoming, Instant.now());
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private MatchLiveEntry buildEntry(Match match) {
        java.util.Optional<LiveMatchSnapshot> snapOpt = stateService.getSnapshot(match.getId());

        if (snapOpt.isPresent()) {
            return fromSnapshot(snapOpt.get());
        }

        // No live state — build a static entry from DB data
        return staticEntry(match);
    }

    private MatchLiveEntry fromSnapshot(LiveMatchSnapshot snap) {
        // Extract latest commentary text from commentary history (last item = most recent)
        String latestCommentary = null;
        if (snap.commentaryHistory() != null && !snap.commentaryHistory().isEmpty()) {
            LiveMatchEvent lastCommentaryEvent = snap.commentaryHistory().get(snap.commentaryHistory().size() - 1);
            if (lastCommentaryEvent.payload() != null) {
                Object raw = lastCommentaryEvent.payload().get("commentary");
                if (raw instanceof String s) {
                    latestCommentary = s;
                } else if (raw instanceof Map<?, ?> m) {
                    Object text = m.get("commentary");
                    if (text instanceof String t) latestCommentary = t;
                }
            }
        }

        return new MatchLiveEntry(
                snap.matchId(),
                snap.homeTeamName(),
                snap.awayTeamName(),
                snap.homeScore(),
                snap.awayScore(),
                snap.currentMinute(),
                null,  // addedTime: expose via extra-time commentary if needed in future
                snap.phase(),
                snap.latestEventType(),
                snap.latestSequence(),
                latestCommentary
        );
    }

    private MatchLiveEntry staticEntry(Match match) {
        LiveMatchPhase phase = switch (match.getStatus() != null ? match.getStatus() : MatchStatus.SCHEDULED) {
            case FINISHED -> LiveMatchPhase.FULL_TIME;
            case LIVE -> LiveMatchPhase.FIRST_HALF;
            default -> LiveMatchPhase.PRE_MATCH;
        };

        return new MatchLiveEntry(
                match.getId(),
                match.getHomeTeam() != null ? match.getHomeTeam().getName() : null,
                match.getAwayTeam() != null ? match.getAwayTeam().getName() : null,
                match.getHomeScore() != null ? match.getHomeScore() : 0,
                match.getAwayScore() != null ? match.getAwayScore() : 0,
                match.getStatus() == MatchStatus.FINISHED ? 90 : null,
                null,
                phase,
                null,
                0,
                null
        );
    }

    private void classify(MatchLiveEntry entry, Match match,
                          List<MatchLiveEntry> active,
                          List<MatchLiveEntry> recentlyCompleted,
                          List<MatchLiveEntry> upcoming) {
        LiveMatchPhase phase = entry.phase();

        if (phase == LiveMatchPhase.FULL_TIME) {
            recentlyCompleted.add(entry);
        } else if (phase != null && phase != LiveMatchPhase.PRE_MATCH) {
            // FIRST_HALF, HALF_TIME, SECOND_HALF, EXTRA_TIME, PENALTY_SHOOTOUT
            active.add(entry);
        } else {
            // PRE_MATCH or DB-only FINISHED matches that have no live state
            if (match.getStatus() == MatchStatus.FINISHED) {
                recentlyCompleted.add(entry);
            } else {
                upcoming.add(entry);
            }
        }
    }
}
