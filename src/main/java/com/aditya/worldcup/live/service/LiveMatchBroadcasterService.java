package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEvent;
import com.aditya.worldcup.live.dto.LiveMatchEventType;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LiveMatchBroadcasterService {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    @Autowired
    public LiveMatchBroadcasterService(SimpMessagingTemplate messagingTemplate,
                                       @Qualifier("liveMatchTaskScheduler") TaskScheduler taskScheduler) {
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = taskScheduler;
    }

    @Value("${live.match.broadcast.delayMs:200}")
    private long broadcastDelayMs;

    private final Map<Long, Boolean> activeBroadcasts = new ConcurrentHashMap<>();
    private final Map<Long, List<ScheduledFuture<?>>> scheduledTasks = new ConcurrentHashMap<>();

    public void broadcastMatch(Long tournamentId, Long matchId, MatchSimulationResponse simulationResult) {
        if (activeBroadcasts.putIfAbsent(matchId, true) != null) {
            log.info("Match {} is already being broadcasted.", matchId);
            return;
        }

        try {
            scheduleBroadcasts(tournamentId, matchId, simulationResult);
        } catch (Exception e) {
            log.error("Failed to schedule live broadcast for match {}", matchId, e);
            cleanup(matchId);
            sendEvent(LiveMatchEvent.create(
                    0, matchId, tournamentId, LiveMatchEventType.ERROR, null, null, null, null, null, null, null, 
                    Map.of("message", "A problem occurred with the live match stream.")
            ));
        }
    }

    private void scheduleBroadcasts(Long tournamentId, Long matchId, MatchSimulationResponse response) {
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

        AtomicInteger sequenceCounter = new AtomicInteger(1);
        AtomicInteger homeScore = new AtomicInteger(0);
        AtomicInteger awayScore = new AtomicInteger(0);

        // Start Event
        tasks.add(taskScheduler.schedule(() -> {
            try {
                sendEvent(LiveMatchEvent.create(
                        sequenceCounter.getAndIncrement(), matchId, tournamentId, LiveMatchEventType.MATCH_STARTED, 0, null, null, null, null, 0, 0, null
                ));
            } catch (Exception ex) {
                log.error("Error broadcasting MATCH_STARTED event for match {}", matchId, ex);
            }
        }, startTime));

        // Kickoff Event
        tasks.add(taskScheduler.schedule(() -> {
            try {
                sendEvent(LiveMatchEvent.create(
                        sequenceCounter.getAndIncrement(), matchId, tournamentId, LiveMatchEventType.KICK_OFF, 0, null, null, null, null, 0, 0, null
                ));
            } catch (Exception ex) {
                log.error("Error broadcasting KICK_OFF event for match {}", matchId, ex);
            }
        }, startTime.plusMillis(broadcastDelayMs)));

        for (int minute = 0; minute <= maxMinute; minute++) {
            final int currentMinute = minute;
            Instant executionTime = startTime.plusMillis((2L + minute) * broadcastDelayMs);
            
            tasks.add(taskScheduler.schedule(() -> {
                try {
                    sendTickEvents(tournamentId, matchId, currentMinute, eventsByMinute.get(currentMinute), commentaryByMinute.get(currentMinute), sequenceCounter, response.homeTeam(), response.awayTeam(), homeScore, awayScore);
                } catch (Exception ex) {
                    log.error("Error broadcasting events for match {}, minute {}", matchId, currentMinute, ex);
                }
            }, executionTime));
        }

        // Finish Event
        final int finalMaxMinute = maxMinute;
        Instant finishTime = startTime.plusMillis((3L + maxMinute) * broadcastDelayMs);
        tasks.add(taskScheduler.schedule(() -> {
            try {
                sendEvent(LiveMatchEvent.create(
                        sequenceCounter.getAndIncrement(), matchId, tournamentId, LiveMatchEventType.FULL_TIME, finalMaxMinute, null, null, null, null, homeScore.get(), awayScore.get(), 
                        Map.of("finalResult", response)
                ));
            } catch (Exception ex) {
                log.error("Error broadcasting FULL_TIME event for match {}", matchId, ex);
            } finally {
                cleanup(matchId);
                log.info("Completed broadcast for match {}", matchId);
            }
        }, finishTime));

        scheduledTasks.put(matchId, tasks);
    }

    private void sendTickEvents(Long tournamentId, Long matchId, int minute, List<MatchEventResponse> evts, List<CommentaryResponse> comms, AtomicInteger seq, String homeTeam, String awayTeam, AtomicInteger homeScore, AtomicInteger awayScore) {
        boolean sentMinuteUpdate = false;
        if (evts != null) {
            for (MatchEventResponse e : evts) {
                LiveMatchEventType type;
                try {
                    type = LiveMatchEventType.valueOf(e.eventType());
                } catch (Exception ex) {
                    type = LiveMatchEventType.MINUTE_UPDATE;
                }
                
                if (type == LiveMatchEventType.GOAL || type == LiveMatchEventType.PENALTY) {
                    if (homeTeam.equals(e.teamName())) {
                        homeScore.incrementAndGet();
                    } else if (awayTeam.equals(e.teamName())) {
                        awayScore.incrementAndGet();
                    }
                } else if (type == LiveMatchEventType.OWN_GOAL) {
                    if (homeTeam.equals(e.teamName())) {
                        awayScore.incrementAndGet(); // other team gets the point
                    } else if (awayTeam.equals(e.teamName())) {
                        homeScore.incrementAndGet();
                    }
                }
                
                sendEvent(LiveMatchEvent.create(
                        seq.getAndIncrement(), matchId, tournamentId, type, minute, null, e.teamId(), e.playerId(), null, homeScore.get(), awayScore.get(), Map.of("matchEvent", e)
                ));
                sentMinuteUpdate = true;
            }
        }
        if (comms != null) {
            for (CommentaryResponse c : comms) {
                sendEvent(LiveMatchEvent.create(
                        seq.getAndIncrement(), matchId, tournamentId, LiveMatchEventType.COMMENTARY, minute, null, null, null, null, homeScore.get(), awayScore.get(), Map.of("commentary", c)
                ));
                sentMinuteUpdate = true;
            }
        }
        
        if (!sentMinuteUpdate) {
            sendEvent(LiveMatchEvent.create(
                    seq.getAndIncrement(), matchId, tournamentId, LiveMatchEventType.MINUTE_UPDATE, minute, null, null, null, null, homeScore.get(), awayScore.get(), null
            ));
        }
    }

    private void sendEvent(LiveMatchEvent payload) {
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
