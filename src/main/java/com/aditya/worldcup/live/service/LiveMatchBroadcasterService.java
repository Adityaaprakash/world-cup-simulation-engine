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

    // Track active match live state
    private final Map<Long, AtomicInteger> sequenceCounters = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> homeScores = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> awayScores = new ConcurrentHashMap<>();

    @Override
    public void publishMatchStarted(Long tournamentId, Long matchId) {
        sequenceCounters.put(matchId, new AtomicInteger(1));
        homeScores.put(matchId, new AtomicInteger(0));
        awayScores.put(matchId, new AtomicInteger(0));

        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.MATCH_STARTED, 0, null, null, null, null, 0, 0, null
        );
        sendEvent(event);
        stateService.applyEvent(event);
    }

    @Override
    public void publishKickOff(Long tournamentId, Long matchId) {
        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.KICK_OFF, 0, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId), null
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

                LiveMatchEvent liveEvt = LiveMatchEvent.create(
                        getSequence(matchId),
                        matchId, tournamentId, type,
                        minute, null,
                        e.teamId(), e.playerId(), null,
                        getHomeScore(matchId), getAwayScore(matchId),
                        Map.of("matchEvent", e)
                );
                sendEvent(liveEvt);
                stateService.applyEvent(liveEvt);
                sentMinuteUpdate = true;
            }
        }
        if (comms != null) {
            for (CommentaryResponse c : comms) {
                LiveMatchEvent commEvt = LiveMatchEvent.create(
                        getSequence(matchId), matchId, tournamentId, LiveMatchEventType.COMMENTARY, minute, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId), Map.of("commentary", c)
                );
                sendEvent(commEvt);
                // Commentary doesn't update score/phase; state update still advances sequence
                stateService.applyEvent(commEvt);
                sentMinuteUpdate = true;
            }
        }

        if (!sentMinuteUpdate) {
            LiveMatchEvent evt = LiveMatchEvent.create(
                    getSequence(matchId), matchId, tournamentId, LiveMatchEventType.MINUTE_UPDATE, minute, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId), null
            );
            sendEvent(evt);
            stateService.applyEvent(evt);
        }
    }

    @Override
    public void publishMatchEnded(Long tournamentId, Long matchId, int finalMinute, MatchSimulationResponse finalResult) {
        LiveMatchEvent event = LiveMatchEvent.create(
                getSequence(matchId), matchId, tournamentId, LiveMatchEventType.FULL_TIME, finalMinute, null, null, null, null, getHomeScore(matchId), getAwayScore(matchId),
                Map.of("finalResult", finalResult)
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
