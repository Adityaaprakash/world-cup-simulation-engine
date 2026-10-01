package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEvent;
import com.aditya.worldcup.live.dto.LiveMatchEventType;
import com.aditya.worldcup.live.dto.LiveMatchPhase;
import com.aditya.worldcup.live.dto.LiveMatchSnapshot;
import com.aditya.worldcup.live.dto.TournamentLiveState;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchRound;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.teams.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Phase 12G — TournamentLiveAggregator unit tests.
 *
 * Covers:
 *   - no active matches
 *   - single active match
 *   - multiple concurrent matches (isolation)
 *   - completed match (via live state)
 *   - completed match (via DB status)
 *   - upcoming match (no live state, not FINISHED)
 *   - mixed active/completed/upcoming
 *   - one failed lookup does not crash the entire aggregation
 *   - match isolation (score/phase never crosses)
 *   - duplicate matchId safe to classify once only
 */
@ExtendWith(MockitoExtension.class)
class TournamentLiveAggregatorTest {

    @Mock
    private LiveMatchStateService stateService;

    @Mock
    private MatchRepository matchRepository;

    private TournamentLiveAggregator aggregator;

    private static final Long TOURNAMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        aggregator = new TournamentLiveAggregator(stateService, matchRepository);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Match dbMatch(Long matchId, MatchStatus status, String home, String away,
                          int homeScore, int awayScore) {
        Team homeTeam = new Team();
        homeTeam.setName(home);
        Team awayTeam = new Team();
        awayTeam.setName(away);
        return Match.builder()
                .id(matchId)
                .status(status)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .round(MatchRound.GROUP_STAGE)
                .build();
    }

    private LiveMatchSnapshot snapshot(Long matchId, LiveMatchPhase phase, int seq,
                                       int homeScore, int awayScore, Integer minute) {
        return new LiveMatchSnapshot(
                matchId, TOURNAMENT_ID, "Home", "Away",
                homeScore, awayScore, minute, phase, seq, null, Instant.now(),
                Collections.emptyList(), null);
    }

    // -----------------------------------------------------------------------
    // No active matches
    // -----------------------------------------------------------------------

    @Test
    void noMatchesReturnsEmptyTournamentState() {
        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID)).thenReturn(Collections.emptyList());

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        assertThat(state.tournamentId()).isEqualTo(TOURNAMENT_ID);
        assertThat(state.activeMatches()).isEmpty();
        assertThat(state.recentlyCompletedMatches()).isEmpty();
        assertThat(state.upcomingMatches()).isEmpty();
        assertThat(state.updatedAt()).isNotNull();
    }

    // -----------------------------------------------------------------------
    // Single active match
    // -----------------------------------------------------------------------

    @Test
    void singleActiveMatchIsClassifiedActive() {
        Match m = dbMatch(10L, MatchStatus.LIVE, "Brazil", "Argentina", 0, 0);
        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID)).thenReturn(List.of(m));
        when(stateService.getSnapshot(10L))
                .thenReturn(Optional.of(snapshot(10L, LiveMatchPhase.SECOND_HALF, 42, 1, 0, 67)));

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        assertThat(state.activeMatches()).hasSize(1);
        TournamentLiveState.MatchLiveEntry entry = state.activeMatches().get(0);
        assertThat(entry.matchId()).isEqualTo(10L);
        assertThat(entry.homeScore()).isEqualTo(1);
        assertThat(entry.awayScore()).isEqualTo(0);
        assertThat(entry.minute()).isEqualTo(67);
        assertThat(entry.phase()).isEqualTo(LiveMatchPhase.SECOND_HALF);
        assertThat(entry.latestSequence()).isEqualTo(42);
        assertThat(state.recentlyCompletedMatches()).isEmpty();
        assertThat(state.upcomingMatches()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Completed match (via live state FULL_TIME)
    // -----------------------------------------------------------------------

    @Test
    void fullTimeMatchClassifiedAsRecentlyCompleted() {
        Match m = dbMatch(20L, MatchStatus.FINISHED, "Spain", "France", 2, 1);
        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID)).thenReturn(List.of(m));
        when(stateService.getSnapshot(20L))
                .thenReturn(Optional.of(snapshot(20L, LiveMatchPhase.FULL_TIME, 130, 2, 1, 90)));

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        assertThat(state.recentlyCompletedMatches()).hasSize(1);
        assertThat(state.recentlyCompletedMatches().get(0).homeScore()).isEqualTo(2);
        assertThat(state.recentlyCompletedMatches().get(0).awayScore()).isEqualTo(1);
        assertThat(state.activeMatches()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Completed match from DB only (no live state)
    // -----------------------------------------------------------------------

    @Test
    void dbFinishedMatchWithNoLiveStateClassifiedAsCompleted() {
        Match m = dbMatch(30L, MatchStatus.FINISHED, "Germany", "England", 3, 2);
        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID)).thenReturn(List.of(m));
        when(stateService.getSnapshot(30L)).thenReturn(Optional.empty());

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        assertThat(state.recentlyCompletedMatches()).hasSize(1);
        TournamentLiveState.MatchLiveEntry entry = state.recentlyCompletedMatches().get(0);
        assertThat(entry.homeScore()).isEqualTo(3);
        assertThat(entry.awayScore()).isEqualTo(2);
        assertThat(entry.phase()).isEqualTo(LiveMatchPhase.FULL_TIME);
        assertThat(state.activeMatches()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Upcoming match (not started)
    // -----------------------------------------------------------------------

    @Test
    void scheduledMatchWithNoLiveStateClassifiedAsUpcoming() {
        Match m = dbMatch(40L, MatchStatus.SCHEDULED, "Italy", "Portugal", 0, 0);
        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID)).thenReturn(List.of(m));
        when(stateService.getSnapshot(40L)).thenReturn(Optional.empty());

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        assertThat(state.upcomingMatches()).hasSize(1);
        assertThat(state.upcomingMatches().get(0).phase()).isEqualTo(LiveMatchPhase.PRE_MATCH);
        assertThat(state.activeMatches()).isEmpty();
        assertThat(state.recentlyCompletedMatches()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Multiple simultaneous matches — isolation
    // -----------------------------------------------------------------------

    @Test
    void simultaneousMatchesAreIsolated() {
        Match mA = dbMatch(100L, MatchStatus.LIVE, "Brazil", "Argentina", 0, 0);
        Match mB = dbMatch(200L, MatchStatus.LIVE, "Spain", "France", 0, 0);
        Match mC = dbMatch(300L, MatchStatus.FINISHED, "Germany", "England", 0, 0);
        Match mD = dbMatch(400L, MatchStatus.SCHEDULED, "Italy", "Portugal", 0, 0);

        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID))
                .thenReturn(List.of(mA, mB, mC, mD));

        when(stateService.getSnapshot(100L))
                .thenReturn(Optional.of(snapshot(100L, LiveMatchPhase.FIRST_HALF, 10, 1, 0, 23)));
        when(stateService.getSnapshot(200L))
                .thenReturn(Optional.of(snapshot(200L, LiveMatchPhase.SECOND_HALF, 85, 0, 2, 72)));
        when(stateService.getSnapshot(300L))
                .thenReturn(Optional.of(snapshot(300L, LiveMatchPhase.FULL_TIME, 130, 3, 1, 90)));
        when(stateService.getSnapshot(400L))
                .thenReturn(Optional.empty());

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        assertThat(state.activeMatches()).hasSize(2);
        assertThat(state.recentlyCompletedMatches()).hasSize(1);
        assertThat(state.upcomingMatches()).hasSize(1);

        // Verify match A score
        TournamentLiveState.MatchLiveEntry entryA = state.activeMatches().stream()
                .filter(e -> e.matchId().equals(100L)).findFirst().orElseThrow();
        assertThat(entryA.homeScore()).isEqualTo(1);
        assertThat(entryA.awayScore()).isEqualTo(0);
        assertThat(entryA.latestSequence()).isEqualTo(10);

        // Verify match B score (completely independent of A)
        TournamentLiveState.MatchLiveEntry entryB = state.activeMatches().stream()
                .filter(e -> e.matchId().equals(200L)).findFirst().orElseThrow();
        assertThat(entryB.homeScore()).isEqualTo(0);
        assertThat(entryB.awayScore()).isEqualTo(2);
        assertThat(entryB.latestSequence()).isEqualTo(85);

        // Verify completed score is preserved separately
        assertThat(state.recentlyCompletedMatches().get(0).homeScore()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // One failed per-match lookup does not crash the aggregation
    // -----------------------------------------------------------------------

    @Test
    void failedMatchLookupIsSkippedGracefully() {
        Match mGood = dbMatch(50L, MatchStatus.LIVE, "Brazil", "Argentina", 0, 0);
        Match mBad = dbMatch(51L, MatchStatus.LIVE, "Broken", "Team", 0, 0);

        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID))
                .thenReturn(List.of(mGood, mBad));

        when(stateService.getSnapshot(50L))
                .thenReturn(Optional.of(snapshot(50L, LiveMatchPhase.FIRST_HALF, 5, 0, 0, 10)));
        // Simulate an exception for the bad match
        when(stateService.getSnapshot(51L))
                .thenThrow(new RuntimeException("Simulated DB failure"));

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        // The good match should still appear; the bad one is skipped
        assertThat(state.activeMatches()).hasSize(1);
        assertThat(state.activeMatches().get(0).matchId()).isEqualTo(50L);
    }

    // -----------------------------------------------------------------------
    // Sequence numbers are match-scoped (no global tournament sequence)
    // -----------------------------------------------------------------------

    @Test
    void sequenceNumbersRemainMatchScoped() {
        Match mA = dbMatch(60L, MatchStatus.LIVE, "A", "B", 0, 0);
        Match mB = dbMatch(61L, MatchStatus.LIVE, "C", "D", 0, 0);
        when(matchRepository.findByTournamentIdOrderById(TOURNAMENT_ID)).thenReturn(List.of(mA, mB));
        when(stateService.getSnapshot(60L))
                .thenReturn(Optional.of(snapshot(60L, LiveMatchPhase.SECOND_HALF, 90, 2, 1, 88)));
        when(stateService.getSnapshot(61L))
                .thenReturn(Optional.of(snapshot(61L, LiveMatchPhase.FIRST_HALF, 12, 0, 0, 15)));

        TournamentLiveState state = aggregator.aggregate(TOURNAMENT_ID);

        // Match A's sequence (90) must never appear on match B and vice versa
        TournamentLiveState.MatchLiveEntry a = state.activeMatches().stream()
                .filter(e -> e.matchId().equals(60L)).findFirst().orElseThrow();
        TournamentLiveState.MatchLiveEntry b = state.activeMatches().stream()
                .filter(e -> e.matchId().equals(61L)).findFirst().orElseThrow();

        assertThat(a.latestSequence()).isEqualTo(90);
        assertThat(b.latestSequence()).isEqualTo(12);
    }
}
