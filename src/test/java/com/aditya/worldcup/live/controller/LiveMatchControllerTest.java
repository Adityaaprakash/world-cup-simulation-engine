package com.aditya.worldcup.live.controller;

import com.aditya.worldcup.live.dto.LiveMatchEventType;
import com.aditya.worldcup.live.dto.LiveMatchPhase;
import com.aditya.worldcup.live.dto.LiveMatchSnapshot;
import com.aditya.worldcup.live.service.LiveMatchStateService;
import com.aditya.worldcup.matches.entity.Match;
import com.aditya.worldcup.matches.entity.MatchStatus;
import com.aditya.worldcup.matches.repository.MatchRepository;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.tournaments.entity.Tournament;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveMatchControllerTest {

    @Mock private LiveMatchStateService stateService;
    @Mock private MatchRepository matchRepository;
    @InjectMocks private LiveMatchController controller;

    private Match buildMatch(Long id, MatchStatus status, Integer homeScore, Integer awayScore) {
        Team home = new Team();
        home.setName("Home FC");
        Team away = new Team();
        away.setName("Away United");

        Tournament t = new Tournament();
        t.setId(1L);

        Match m = new Match();
        m.setId(id);
        m.setStatus(status);
        m.setHomeTeam(home);
        m.setAwayTeam(away);
        m.setHomeScore(homeScore);
        m.setAwayScore(awayScore);
        m.setTournament(t);
        return m;
    }

    @Test
    void returnsLiveSnapshotFromStateService() {
        Match match = buildMatch(100L, MatchStatus.LIVE, null, null);
        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));

        LiveMatchSnapshot snapshot = new LiveMatchSnapshot(
                100L, 1L, "Home FC", "Away United", 1, 0,
                35, LiveMatchPhase.FIRST_HALF, 45, LiveMatchEventType.GOAL, Instant.now(),
                java.util.Collections.emptyList()
        );
        when(stateService.getSnapshot(100L)).thenReturn(Optional.of(snapshot));

        ResponseEntity<LiveMatchSnapshot> resp = controller.getLiveMatchSnapshot(100L);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().homeScore()).isEqualTo(1);
        assertThat(resp.getBody().phase()).isEqualTo(LiveMatchPhase.FIRST_HALF);
        assertThat(resp.getBody().latestSequence()).isEqualTo(45);
    }

    @Test
    void returnsStaticPreMatchSnapshotWhenNotYetLive() {
        Match match = buildMatch(200L, MatchStatus.SCHEDULED, null, null);
        when(matchRepository.findById(200L)).thenReturn(Optional.of(match));
        when(stateService.getSnapshot(200L)).thenReturn(Optional.empty());

        ResponseEntity<LiveMatchSnapshot> resp = controller.getLiveMatchSnapshot(200L);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        LiveMatchSnapshot body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.phase()).isEqualTo(LiveMatchPhase.PRE_MATCH);
        assertThat(body.latestSequence()).isEqualTo(0);
    }

    @Test
    void returnsFullTimeSnapshotForFinishedMatch() {
        Match match = buildMatch(300L, MatchStatus.FINISHED, 2, 1);
        when(matchRepository.findById(300L)).thenReturn(Optional.of(match));
        when(stateService.getSnapshot(300L)).thenReturn(Optional.empty());

        ResponseEntity<LiveMatchSnapshot> resp = controller.getLiveMatchSnapshot(300L);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        LiveMatchSnapshot body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.phase()).isEqualTo(LiveMatchPhase.FULL_TIME);
        assertThat(body.homeScore()).isEqualTo(2);
        assertThat(body.awayScore()).isEqualTo(1);
    }

    @Test
    void throwsForUnknownMatchId() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> controller.getLiveMatchSnapshot(999L));
    }
}
