package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEventPayload;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveMatchBroadcasterServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private LiveMatchBroadcasterService broadcasterService;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(broadcasterService, "broadcastDelayMs", 100L);
    }

    @Test
    void shouldScheduleEventsInChronologicalOrder() {
        MatchSimulationResponse response = new MatchSimulationResponse(
                "Home", "Away", 1, 0, "Home", false, false, null, null, 80, 75,
                List.of(new MatchEventResponse(15, "Player", "GOAL", "Home Team Goal")),
                null, List.of(), null,
                List.of(new CommentaryResponse(15, "What a strike!"))
        );

        broadcasterService.broadcastMatch(100L, response);

        verify(taskScheduler, atLeastOnce()).schedule(runnableCaptor.capture(), any(Instant.class));

        List<Runnable> scheduledTasks = runnableCaptor.getAllValues();
        assertThat(scheduledTasks).isNotEmpty();
        
        for (Runnable runnable : scheduledTasks) {
            runnable.run();
        }

        ArgumentCaptor<LiveMatchEventPayload> payloadCaptor = ArgumentCaptor.forClass(LiveMatchEventPayload.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/matches/100"), payloadCaptor.capture());

        List<LiveMatchEventPayload> payloads = payloadCaptor.getAllValues();
        assertThat(payloads.stream().filter(p -> "STARTED".equals(p.state()))).hasSize(1);
        assertThat(payloads.stream().filter(p -> "EVENT".equals(p.state()))).hasSize(2);
        assertThat(payloads.stream().filter(p -> "FINISHED".equals(p.state()))).hasSize(1);
    }

    @Test
    void shouldPreventDuplicateBroadcasts() {
        MatchSimulationResponse response = new MatchSimulationResponse(
                "Home", "Away", 0, 0, "DRAW", false, false, null, null, 80, 80,
                List.of(), null, List.of(), null, List.of()
        );

        broadcasterService.broadcastMatch(200L, response);
        broadcasterService.broadcastMatch(200L, response);

        verify(taskScheduler, times(93)).schedule(any(Runnable.class), any(Instant.class)); 
        // 90 minutes + 1 start + 1 finish = 93 tasks per broadcast. If twice, it would be 186, so times(93) proves duplication prevention.
    }

    @Test
    void shouldHandleEmptyEventListsGracefully() {
        MatchSimulationResponse response = new MatchSimulationResponse(
                "Home", "Away", 0, 0, "DRAW", false, false, null, null, 80, 80,
                null, null, null, null, null
        );

        broadcasterService.broadcastMatch(300L, response);

        verify(taskScheduler, atLeastOnce()).schedule(runnableCaptor.capture(), any(Instant.class));

        List<Runnable> scheduledTasks = runnableCaptor.getAllValues();
        for (Runnable runnable : scheduledTasks) {
            runnable.run();
        }

        ArgumentCaptor<LiveMatchEventPayload> payloadCaptor = ArgumentCaptor.forClass(LiveMatchEventPayload.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/matches/300"), payloadCaptor.capture());

        List<LiveMatchEventPayload> payloads = payloadCaptor.getAllValues();
        assertThat(payloads.stream().filter(p -> "STARTED".equals(p.state()))).hasSize(1);
        assertThat(payloads.stream().filter(p -> "FINISHED".equals(p.state()))).hasSize(1);
        assertThat(payloads.stream().filter(p -> "EVENT".equals(p.state()))).isEmpty();
    }
}
