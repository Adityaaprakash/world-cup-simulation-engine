package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEventPayload;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveMatchBroadcasterService {

    private final SimpMessagingTemplate messagingTemplate;
    
    @Qualifier("liveMatchTaskScheduler")
    private final TaskScheduler taskScheduler;

    @Value("${live.match.broadcast.delayMs:200}")
    private long broadcastDelayMs;

    private final Map<Long, Boolean> activeBroadcasts = new ConcurrentHashMap<>();
    private final Map<Long, List<ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>();

    public void broadcastMatch(Long matchId, MatchSimulationResponse simulationResult) {
        if (activeBroadcasts.putIfAbsent(matchId, true) != null) {
            log.info("Match {} is already being broadcasted.", matchId);
            return;
        }

        try {
            scheduleBroadcasts(matchId, simulationResult);
        } catch (Exception e) {
            log.error("Failed to schedule live broadcast for match {}", matchId, e);
            cleanup(matchId);
            sendPayload(LiveMatchEventPayload.error(matchId));
        }
    }

    private void scheduleBroadcasts(Long matchId, MatchSimulationResponse response) {
        log.info("Scheduling live broadcast for match {} starting shortly.", matchId);
        
        List<MatchEventResponse> events = response.events() == null ? new ArrayList<>() : new ArrayList<>(response.events());
        List<CommentaryResponse> commentary = response.commentary() == null ? new ArrayList<>() : new ArrayList<>(response.commentary());
        
        // Group by minute
        Map<Integer, List<MatchEventResponse>> eventsByMinute = events.stream()
                .filter(e -> e.minute() != null)
                .collect(Collectors.groupingBy(MatchEventResponse::minute));
                
        Map<Integer, List<CommentaryResponse>> commentaryByMinute = commentary.stream()
                .filter(c -> c.minute() != null)
                .collect(Collectors.groupingBy(CommentaryResponse::minute));

        int maxEventMinute = eventsByMinute.keySet().stream().max(Integer::compareTo).orElse(0);
        int maxCommentaryMinute = commentaryByMinute.keySet().stream().max(Integer::compareTo).orElse(0);
        int maxMinute = Math.max(90, Math.max(maxEventMinute, maxCommentaryMinute));

        // Sort events within the same minute natively using exact existing list iteration logic,
        // although MatchEventResponse does not possess an internal sequence ID other than list order.
        // We will output elements safely chronologically matching original generation array sequences.
        List<ScheduledFuture<?>> tasks = new ArrayList<>();

        Instant startTime = Instant.now().plusMillis(broadcastDelayMs); // small initial delay

        // Start Event
        tasks.add(taskScheduler.schedule(() -> sendPayload(LiveMatchEventPayload.started(matchId)), startTime));

        for (int minute = 0; minute <= maxMinute; minute++) {
            final int currentMinute = minute;
            Instant executionTime = startTime.plusMillis(broadcastDelayMs + (minute * broadcastDelayMs));
            
            tasks.add(taskScheduler.schedule(() -> {
                sendTickEvents(matchId, currentMinute, eventsByMinute.get(currentMinute), commentaryByMinute.get(currentMinute));
            }, executionTime));
        }

        // Finish Event
        final int finalMaxMinute = maxMinute;
        Instant finishTime = startTime.plusMillis(broadcastDelayMs + ((maxMinute + 1) * broadcastDelayMs));
        tasks.add(taskScheduler.schedule(() -> {
            sendPayload(LiveMatchEventPayload.finished(matchId, response, finalMaxMinute));
            cleanup(matchId);
            log.info("Completed broadcast for match {}", matchId);
        }, finishTime));

        scheduledTasks.put(matchId, tasks);
    }

    private void sendTickEvents(Long matchId, int minute, List<MatchEventResponse> evts, List<CommentaryResponse> comms) {
        if (evts != null) {
            evts.forEach(e -> sendPayload(LiveMatchEventPayload.event(matchId, minute, e, null)));
        }
        if (comms != null) {
            comms.forEach(c -> sendPayload(LiveMatchEventPayload.event(matchId, minute, null, c)));
        }
    }

    private void sendPayload(LiveMatchEventPayload payload) {
        messagingTemplate.convertAndSend("/topic/matches/" + payload.matchId(), payload);
    }
    
    public void cleanup(Long matchId) {
        List<ScheduledFuture<?>> tasks = scheduledTasks.remove(matchId);
        if (tasks != null) {
            tasks.stream().filter(java.util.Objects::nonNull).forEach(t -> t.cancel(false));
        }
        activeBroadcasts.remove(matchId);
    }
}
