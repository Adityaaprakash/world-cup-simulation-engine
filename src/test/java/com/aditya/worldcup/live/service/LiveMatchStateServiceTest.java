package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEvent;
import com.aditya.worldcup.live.dto.LiveMatchEventType;
import com.aditya.worldcup.live.dto.LiveMatchPhase;
import com.aditya.worldcup.live.dto.LiveMatchSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class LiveMatchStateServiceTest {

    private LiveMatchStateService service;

    @BeforeEach
    void setUp() {
        service = new LiveMatchStateService();
    }

    // -----------------------------------------------------------------------
    // Helper: build a minimal LiveMatchEvent
    // -----------------------------------------------------------------------
    private LiveMatchEvent event(int seq, Long matchId, LiveMatchEventType type,
                                 Integer minute, Integer homeScore, Integer awayScore) {
        return LiveMatchEvent.create(seq, matchId, 1L, type, minute, null, null, null, null, homeScore, awayScore, null);
    }

    // -----------------------------------------------------------------------
    // Snapshot creation
    // -----------------------------------------------------------------------

    @Test
    void initialEventCreatesSnapshot() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));

        Optional<LiveMatchSnapshot> snap = service.getSnapshot(100L);
        assertThat(snap).isPresent();
        assertThat(snap.get().matchId()).isEqualTo(100L);
        assertThat(snap.get().homeScore()).isEqualTo(0);
        assertThat(snap.get().awayScore()).isEqualTo(0);
        assertThat(snap.get().phase()).isEqualTo(LiveMatchPhase.PRE_MATCH);
        assertThat(snap.get().latestSequence()).isEqualTo(1);
    }

    @Test
    void noSnapshotBeforeMatchStarted() {
        assertThat(service.getSnapshot(999L)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // KICK_OFF transitions to FIRST_HALF
    // -----------------------------------------------------------------------

    @Test
    void kickOffSetsFirstHalfPhase() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(2, 100L, LiveMatchEventType.KICK_OFF, 0, 0, 0));

        LiveMatchSnapshot snap = service.getSnapshot(100L).orElseThrow();
        assertThat(snap.phase()).isEqualTo(LiveMatchPhase.FIRST_HALF);
        assertThat(snap.latestSequence()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Goal updates score
    // -----------------------------------------------------------------------

    @Test
    void goalEventIncrementsScore() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(2, 100L, LiveMatchEventType.KICK_OFF, 0, 0, 0));
        service.applyEvent(event(3, 100L, LiveMatchEventType.MINUTE_UPDATE, 10, 0, 0));
        service.applyEvent(event(4, 100L, LiveMatchEventType.GOAL, 25, 1, 0));

        LiveMatchSnapshot snap = service.getSnapshot(100L).orElseThrow();
        assertThat(snap.homeScore()).isEqualTo(1);
        assertThat(snap.awayScore()).isEqualTo(0);
        assertThat(snap.latestSequence()).isEqualTo(4);
    }

    // -----------------------------------------------------------------------
    // Sequence ordering
    // -----------------------------------------------------------------------

    @Test
    void sequentialEventsAdvanceSequence() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(2, 100L, LiveMatchEventType.KICK_OFF, 0, 0, 0));
        service.applyEvent(event(3, 100L, LiveMatchEventType.MINUTE_UPDATE, 5, 0, 0));

        assertThat(service.getSnapshot(100L).orElseThrow().latestSequence()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Duplicate event — must NOT mutate state twice
    // -----------------------------------------------------------------------

    @Test
    void duplicateEventIsIdempotent() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(2, 100L, LiveMatchEventType.KICK_OFF, 0, 0, 0));
        service.applyEvent(event(3, 100L, LiveMatchEventType.GOAL, 30, 1, 0));
        // Apply seq=3 again — score must NOT become 2-0
        service.applyEvent(event(3, 100L, LiveMatchEventType.GOAL, 30, 2, 0));

        LiveMatchSnapshot snap = service.getSnapshot(100L).orElseThrow();
        assertThat(snap.homeScore()).isEqualTo(1);   // first application wins
        assertThat(snap.latestSequence()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Out-of-order event — late arrival of older event must be discarded
    // -----------------------------------------------------------------------

    @Test
    void outOfOrderEventIsDiscarded() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(5, 100L, LiveMatchEventType.GOAL, 40, 2, 1));
        // Now deliver seq=4 retroactively — must be ignored
        service.applyEvent(event(4, 100L, LiveMatchEventType.MINUTE_UPDATE, 39, 1, 1));

        LiveMatchSnapshot snap = service.getSnapshot(100L).orElseThrow();
        assertThat(snap.latestSequence()).isEqualTo(5);
        assertThat(snap.homeScore()).isEqualTo(2);   // seq=5 result preserved
    }

    // -----------------------------------------------------------------------
    // Full match lifecycle
    // -----------------------------------------------------------------------

    @Test
    void fullMatchLifecycleTransitionsPhases() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        assertThat(service.getSnapshot(100L).orElseThrow().phase()).isEqualTo(LiveMatchPhase.PRE_MATCH);

        service.applyEvent(event(2, 100L, LiveMatchEventType.KICK_OFF, 0, 0, 0));
        assertThat(service.getSnapshot(100L).orElseThrow().phase()).isEqualTo(LiveMatchPhase.FIRST_HALF);

        service.applyEvent(event(46, 100L, LiveMatchEventType.HALF_TIME, 45, 0, 0));
        assertThat(service.getSnapshot(100L).orElseThrow().phase()).isEqualTo(LiveMatchPhase.HALF_TIME);

        service.applyEvent(event(47, 100L, LiveMatchEventType.SECOND_HALF_STARTED, 45, 0, 0));
        assertThat(service.getSnapshot(100L).orElseThrow().phase()).isEqualTo(LiveMatchPhase.SECOND_HALF);

        service.applyEvent(event(137, 100L, LiveMatchEventType.FULL_TIME, 90, 1, 0));
        assertThat(service.getSnapshot(100L).orElseThrow().phase()).isEqualTo(LiveMatchPhase.FULL_TIME);
    }

    // -----------------------------------------------------------------------
    // Extra time
    // -----------------------------------------------------------------------

    @Test
    void extraTimePhaseTransition() {
        service.applyEvent(event(1, 200L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(2, 200L, LiveMatchEventType.KICK_OFF, 0, 0, 0));
        service.applyEvent(event(150, 200L, LiveMatchEventType.EXTRA_TIME_STARTED, 90, 0, 0));

        assertThat(service.getSnapshot(200L).orElseThrow().phase()).isEqualTo(LiveMatchPhase.EXTRA_TIME);
    }

    // -----------------------------------------------------------------------
    // Penalty shootout
    // -----------------------------------------------------------------------

    @Test
    void penaltyShootoutPhaseTransition() {
        service.applyEvent(event(1, 300L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(2, 300L, LiveMatchEventType.KICK_OFF, 0, 0, 0));
        service.applyEvent(event(200, 300L, LiveMatchEventType.PENALTY_SHOOTOUT_STARTED, 120, 0, 0));
        service.applyEvent(event(201, 300L, LiveMatchEventType.PENALTY_SCORED, 120, 1, 0));
        service.applyEvent(event(210, 300L, LiveMatchEventType.FULL_TIME, 120, 4, 3));

        LiveMatchSnapshot snap = service.getSnapshot(300L).orElseThrow();
        assertThat(snap.phase()).isEqualTo(LiveMatchPhase.FULL_TIME);
        assertThat(snap.homeScore()).isEqualTo(4);
        assertThat(snap.awayScore()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Concurrent matches — state for match A must not affect match B
    // -----------------------------------------------------------------------

    @Test
    void concurrentMatchesAreIsolated() {
        service.applyEvent(event(1, 10L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.applyEvent(event(1, 20L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));

        service.applyEvent(event(2, 10L, LiveMatchEventType.GOAL, 15, 1, 0));
        service.applyEvent(event(2, 20L, LiveMatchEventType.GOAL, 20, 0, 2));

        LiveMatchSnapshot snapA = service.getSnapshot(10L).orElseThrow();
        LiveMatchSnapshot snapB = service.getSnapshot(20L).orElseThrow();

        assertThat(snapA.homeScore()).isEqualTo(1);
        assertThat(snapA.awayScore()).isEqualTo(0);
        assertThat(snapB.homeScore()).isEqualTo(0);
        assertThat(snapB.awayScore()).isEqualTo(2);
    }

    // -----------------------------------------------------------------------
    // Thread-safety under concurrent writes
    // -----------------------------------------------------------------------

    @Test
    void concurrentEventsAreThreadSafe() throws InterruptedException {
        service.applyEvent(event(1, 50L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));

        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 2; i <= threads + 1; i++) {
            final int seq = i;
            pool.submit(() -> {
                service.applyEvent(event(seq, 50L, LiveMatchEventType.MINUTE_UPDATE, seq, 0, 0));
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        LiveMatchSnapshot snap = service.getSnapshot(50L).orElseThrow();
        // The highest sequence wins; score stays 0-0
        assertThat(snap.latestSequence()).isGreaterThanOrEqualTo(2);
        assertThat(snap.homeScore()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Expire
    // -----------------------------------------------------------------------

    @Test
    void expireRemovesState() {
        service.applyEvent(event(1, 100L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        assertThat(service.isLive(100L)).isTrue();

        service.expireMatch(100L);
        assertThat(service.isLive(100L)).isFalse();
        assertThat(service.getSnapshot(100L)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Team name seeding
    // -----------------------------------------------------------------------

    @Test
    void seedTeamNamesAreReflectedInSnapshot() {
        service.applyEvent(event(1, 400L, LiveMatchEventType.MATCH_STARTED, 0, 0, 0));
        service.seedTeamNames(400L, "Brazil", "Argentina");

        LiveMatchSnapshot snap = service.getSnapshot(400L).orElseThrow();
        assertThat(snap.homeTeamName()).isEqualTo("Brazil");
        assertThat(snap.awayTeamName()).isEqualTo("Argentina");
    }
}
