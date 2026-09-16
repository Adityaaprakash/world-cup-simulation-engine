package com.aditya.worldcup.live.dto;

import com.aditya.worldcup.matchevents.dto.MatchEventResponse;
import com.aditya.worldcup.simulation.dto.CommentaryResponse;
import com.aditya.worldcup.simulation.dto.MatchSimulationResponse;

public record LiveMatchEventPayload(
        Long matchId,
        String state,
        Integer minute,
        MatchEventResponse matchEvent,
        CommentaryResponse commentary,
        MatchSimulationResponse finalResult
) {
    public static LiveMatchEventPayload started(Long matchId) {
        return new LiveMatchEventPayload(matchId, "STARTED", 0, null, null, null);
    }

    public static LiveMatchEventPayload event(Long matchId, Integer minute, MatchEventResponse matchEvent, CommentaryResponse commentary) {
        return new LiveMatchEventPayload(matchId, "EVENT", minute, matchEvent, commentary, null);
    }

    public static LiveMatchEventPayload finished(Long matchId, MatchSimulationResponse finalResult, Integer finishMinute) {
        return new LiveMatchEventPayload(matchId, "FINISHED", finishMinute, null, null, finalResult);
    }

    public static LiveMatchEventPayload error(Long matchId) {
        return new LiveMatchEventPayload(matchId, "ERROR", null, null, null, null);
    }
}
