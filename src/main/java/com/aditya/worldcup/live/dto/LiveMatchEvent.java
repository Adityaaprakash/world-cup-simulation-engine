package com.aditya.worldcup.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveMatchEvent(
        String eventId,
        int sequenceNumber,
        Long matchId,
        Long tournamentId,
        LiveMatchEventType eventType,
        Integer minute,
        Integer addedTime,
        Long teamId,
        Long playerId,
        Long secondaryPlayerId,
        Integer homeScore,
        Integer awayScore,
        Instant timestamp,
        Map<String, Object> payload
) {
    public static LiveMatchEvent create(
            int sequenceNumber,
            Long matchId,
            Long tournamentId,
            LiveMatchEventType eventType,
            Integer minute,
            Integer addedTime,
            Long teamId,
            Long playerId,
            Long secondaryPlayerId,
            Integer homeScore,
            Integer awayScore,
            Map<String, Object> payload
    ) {
        return new LiveMatchEvent(
                UUID.randomUUID().toString(),
                sequenceNumber,
                matchId,
                tournamentId,
                eventType,
                minute,
                addedTime,
                teamId,
                playerId,
                secondaryPlayerId,
                homeScore,
                awayScore,
                Instant.now(),
                payload
        );
    }
}
