package com.aditya.worldcup.live.engine;

import com.aditya.worldcup.live.publisher.LiveEventPublisher;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class LiveSimulationEngine {

    private final TaskScheduler taskScheduler;
    private final LiveEventPublisher publisher;

    @Value("${live.match.broadcast.delayMs:200}")
    private long broadcastDelayMs;

    private final Map<Long, List<ScheduledFuture<?>>> activeSimulations = new ConcurrentHashMap<>();

    public LiveSimulationEngine(@Qualifier("liveMatchTaskScheduler") TaskScheduler taskScheduler,
                                LiveEventPublisher publisher) {
        this.taskScheduler = taskScheduler;
        this.publisher = publisher;
    }

    public void startLiveSimulation(Long tournamentId, Long matchId, MatchSimulationResponse response) {
        if (activeSimulations.containsKey(matchId)) {
            log.info("Simulation for match {} is already progressing.", matchId);
            return;
        }

        log.info("Starting progressive live simulation for match {}", matchId);
        List<ScheduledFuture<?>> tasks = new ArrayList<>();

        List<MatchEventResponse> events = response.events() == null ? new ArrayList<>() : new ArrayList<>(response.events());
        List<CommentaryResponse> commentary = response.commentary() == null ? new ArrayList<>() : new ArrayList<>(response.commentary());

        Map<Integer, List<MatchEventResponse>> eventsByMinute = events.stream()
                .filter(e -> e.minute() != null)
                .collect(Collectors.groupingBy(MatchEventResponse::minute));

        Map<Integer, List<CommentaryResponse>> commentaryByMinute = commentary.stream()
                .filter(c -> c.minute() != null)
                .collect(Collectors.groupingBy(CommentaryResponse::minute));

        int maxEventMinute = eventsByMinute.keySet().stream().max(Integer::compareTo).orElse(0);
        int maxCommentaryMinute = commentaryByMinute.keySet().stream().max(Integer::compareTo).orElse(0);
        int maxMinute = Math.max(90, Math.max(maxEventMinute, maxCommentaryMinute));

        Instant startTime = Instant.now().plusMillis(broadcastDelayMs);

        tasks.add(taskScheduler.schedule(() -> {
            try {
                publisher.publishMatchStarted(tournamentId, matchId);
            } catch (Exception ex) {
                publisher.publishError(tournamentId, matchId, "Error publishing MATCH_STARTED: " + ex.getMessage());
            }
        }, startTime));

        tasks.add(taskScheduler.schedule(() -> {
            try {
                publisher.publishKickOff(tournamentId, matchId);
            } catch (Exception ex) {
                publisher.publishError(tournamentId, matchId, "Error publishing KICK_OFF: " + ex.getMessage());
            }
        }, startTime.plusMillis(broadcastDelayMs)));

        for (int minute = 0; minute <= maxMinute; minute++) {
            final int currentMinute = minute;
            Instant executionTime = startTime.plusMillis((2L + minute) * broadcastDelayMs);

            tasks.add(taskScheduler.schedule(() -> {
                try {
                    publisher.publishMinuteTick(tournamentId, matchId, currentMinute,
                        eventsByMinute.get(currentMinute),
                        commentaryByMinute.get(currentMinute),
                        response.homeTeam(),
                        response.awayTeam());
                } catch (Exception ex) {
                    publisher.publishError(tournamentId, matchId, "Error during minute tick " + currentMinute + ": " + ex.getMessage());
                }
            }, executionTime));
        }

        final int finalMaxMinute = maxMinute;
        Instant finishTime = startTime.plusMillis((3L + maxMinute) * broadcastDelayMs);
        tasks.add(taskScheduler.schedule(() -> {
            try {
                publisher.publishMatchEnded(tournamentId, matchId, finalMaxMinute, response);
            } catch (Exception ex) {
                publisher.publishError(tournamentId, matchId, "Error publishing FULL_TIME: " + ex.getMessage());
            } finally {
                cancelSimulation(matchId);
            }
        }, finishTime));

        activeSimulations.put(matchId, tasks);
    }

    public void cancelSimulation(Long matchId) {
        List<ScheduledFuture<?>> tasks = activeSimulations.remove(matchId);
        if (tasks != null) {
            tasks.stream().filter(java.util.Objects::nonNull).forEach(t -> t.cancel(false));
            log.info("Live progressive simulation for match {} has ended.", matchId);
        }
    }
}
