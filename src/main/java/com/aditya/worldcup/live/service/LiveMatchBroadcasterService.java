package com.aditya.worldcup.live.service;

import com.aditya.worldcup.live.dto.LiveMatchEvent;
import com.aditya.worldcup.live.dto.LiveMatchEventType;
import com.aditya.worldcup.live.publisher.LiveEventPublisher;
import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiveMatchBroadcasterService implements LiveEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final LiveMatchStateService stateService;
    private final com.aditya.worldcup.simulation.service.MatchCommentaryService matchCommentaryService;

    // Track active match live state
    private final Map<Long, AtomicInteger> sequenceCounters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> homeScores = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> awayScores = new ConcurrentHashMap<>();

    @Override
    public void publishMatchStarted(Long tournamentId, Long matchId) {
        sequenceCounters.put(matchId, new AtomicInteger(1));
        homeScores.put(matchId, new AtomicInteger(0));
        awayScores.put(matchId, new AtomicInteger(0));

        Map<String, Object> payload = new java.util.HashMap<>();
        String comm = matchCommentaryService.generateLifecycleCommentary(LiveMatchEventType.MATCH_STARTED, "", "", 0, 0);
        if (comm != null) payload.put("commentary", comm);

        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.MATCH_STARTED, 0, null, null, null, null, 0, 0,
                payload.isEmpty() ? null : payload
        );
        sendEvent(event);
        stateService.applyEvent(event);
    }

    @Override
    public void publishKickOff(Long tournamentId, Long matchId) {
        Map<String, Object> payload = new java.util.HashMap<>();
        String comm = matchCommentaryService.generateLifecycleCommentary(LiveMatchEventType.KICK_OFF, "", "", 0, 0);
        if (comm != null) payload.put("commentary", comm);

        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.KICK_OFF, 0, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                payload.isEmpty() ? null : payload
        );
        sendEvent(event);
        stateService.applyEvent(event);
    }

    @Override
    public void publishMinuteTick(Long tournamentId, Long matchId, int minute,
                                  List<MatchEventResponse> evts,
                                  List<CommentaryResponse> comms,
                                  String homeTeam, String awayTeam) {

        boolean sentMinuteUpdate = false;

        // Determine base minute and added time
        int baseMinute = minute;
        Integer addedTime = null;
        if (minute > 45 && minute < 50) {
            // First half stoppage time is not tracked well, mostly it just jumps to second half or ends.
            // But we don't have enough context.
        }
        if (minute > 90 && minute < 105) {
            baseMinute = 90;
            addedTime = minute - 90;
        } else if (minute > 105 && minute < 120) {
            // extra time first half
            baseMinute = 105;
            addedTime = minute - 105;
        } else if (minute > 120) {
            baseMinute = 120;
            addedTime = minute - 120;
        }

        if (minute == 45) {
            Map<String, Object> payload = new java.util.HashMap<>();
            String comm = matchCommentaryService.generateLifecycleCommentary(LiveMatchEventType.HALF_TIME, homeTeam, awayTeam, getHomeScore(matchId), getAwayScore(matchId));
            if (comm != null) payload.put("commentary", comm);
            LiveMatchEvent htEvent = LiveMatchEvent.create(
                    getSequence(matchId), matchId, tournamentId, LiveMatchEventType.HALF_TIME, 45, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                    payload.isEmpty() ? null : payload
            );
            sendEvent(htEvent);
            stateService.applyEvent(htEvent);
        } else if (minute == 46) {
            Map<String, Object> payload = new java.util.HashMap<>();
            String comm = matchCommentaryService.generateLifecycleCommentary(LiveMatchEventType.SECOND_HALF_STARTED, homeTeam, awayTeam, getHomeScore(matchId), getAwayScore(matchId));
            if (comm != null) payload.put("commentary", comm);
            LiveMatchEvent htEvent = LiveMatchEvent.create(
                    getSequence(matchId), matchId, tournamentId, LiveMatchEventType.SECOND_HALF_STARTED, 45, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                    payload.isEmpty() ? null : payload
            );
            sendEvent(htEvent);
            stateService.applyEvent(htEvent);
        } else if (minute == 91) {
            Map<String, Object> payload = new java.util.HashMap<>();
            String comm = matchCommentaryService.generateLifecycleCommentary(LiveMatchEventType.EXTRA_TIME_STARTED, homeTeam, awayTeam, getHomeScore(matchId), getAwayScore(matchId));
            if (comm != null) payload.put("commentary", comm);
            LiveMatchEvent htEvent = LiveMatchEvent.create(
                    getSequence(matchId), matchId, tournamentId, LiveMatchEventType.EXTRA_TIME_STARTED, 90, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                    payload.isEmpty() ? null : payload
            );
            sendEvent(htEvent);
            stateService.applyEvent(htEvent);
        } else if (minute == 120) {
            // maybe shootout started
        }

        if (evts != null) {
            for (MatchEventResponse e : evts) {
                LiveMatchEventType type;
                try {
                    type = LiveMatchEventType.valueOf(e.eventType());
                } catch (Exception ex) {
                    type = LiveMatchEventType.MINUTE_UPDATE;
                }

                if (type == LiveMatchEventType.GOAL || type == LiveMatchEventType.PENALTY || type == LiveMatchEventType.PENALTY_SCORED) {
                    if (homeTeam.equals(e.teamName())) {
                        incrementHomeScore(matchId);
                    } else if (awayTeam.equals(e.teamName())) {
                        incrementAwayScore(matchId);
                    }
                } else if (type == LiveMatchEventType.OWN_GOAL) {
                    if (homeTeam.equals(e.teamName())) {
                        incrementAwayScore(matchId);
                    } else if (awayTeam.equals(e.teamName())) {
                        incrementHomeScore(matchId);
                    }
                }

                String commentary = matchCommentaryService.createCommentary(e);
                Map<String, Object> payloadMap = new java.util.HashMap<>();
                payloadMap.put("matchEvent", e);
                if (commentary != null) {
                    payloadMap.put("commentary", commentary);
                }

                LiveMatchEvent liveEvt = LiveMatchEvent.create(
                        getSequence(matchId),
                        matchId, tournamentId, type,
                        baseMinute, addedTime,
                        e.teamId(), e.playerId(), null,
                        getHomeScore(matchId), getAwayScore(matchId),
                        payloadMap
                );
                sendEvent(liveEvt);
                stateService.applyEvent(liveEvt);
                sentMinuteUpdate = true;
            }
        }

        if (!sentMinuteUpdate) {
            LiveMatchEvent evt = LiveMatchEvent.create(
                    getSequence(matchId), matchId, tournamentId, LiveMatchEventType.MINUTE_UPDATE, baseMinute, addedTime, null, null, null, getHomeScore(matchId), getAwayScore(matchId), null
            );
            sendEvent(evt);
            stateService.applyEvent(evt);
        }
    }

    @Override
    public void publishMatchEnded(Long tournamentId, Long matchId, int finalMinute, MatchSimulationResponse finalResult) {
        Map<String, Object> payloadMap = new java.util.HashMap<>();
        payloadMap.put("finalResult", finalResult);
        String comm = matchCommentaryService.generateLifecycleCommentary(LiveMatchEventType.FULL_TIME, finalResult.homeTeam(), finalResult.awayTeam(), getHomeScore(matchId), getAwayScore(matchId));
        if (comm != null) payloadMap.put("commentary", comm);

        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.FULL_TIME, finalMinute, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                payloadMap
        );
        // Seed team names into state before applying FULL_TIME
        stateService.seedTeamNames(matchId, finalResult.homeTeam(), finalResult.awayTeam());
        sendEvent(event);
        stateService.applyEvent(event);
        cleanup(matchId);
    }

    @Override
    public void publishError(Long tournamentId, Long matchId, String message) {
        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.ERROR, null, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                Map.of("message", message)
        );
        sendEvent(event);
        stateService.applyEvent(event);
        cleanup(matchId);
    }

    private void sendEvent(LiveMatchEvent payload) {
        messagingTemplate.convertAndSend("/topic/matches/" + payload.matchId(), payload);
    }

    private int getSequence(Long matchId) {
        return sequenceCounters.getOrDefault(matchId, new AtomicInteger(1)).getAndIncrement();
    }

    private void incrementHomeScore(Long matchId) {
        if (homeScores.containsKey(matchId)) {
            homeScores.get(matchId).incrementAndGet();
        }
    }

    private void incrementAwayScore(Long matchId) {
        if (awayScores.containsKey(matchId)) {
            awayScores.get(matchId).incrementAndGet();
        }
    }

    private int getHomeScore(Long matchId) {
        return homeScores.containsKey(matchId) ? homeScores.get(matchId).get() : 0;
    }

    private int getAwayScore(Long matchId) {
        return awayScores.containsKey(matchId) ? awayScores.get(matchId).get() : 0;
    }

    private void cleanup(Long matchId) {
        sequenceCounters.remove(matchId);
        homeScores.remove(matchId);
        awayScores.remove(matchId);
    }
}
