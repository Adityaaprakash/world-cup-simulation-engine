package com.aditya.worldcup.live.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Backend-authoritative snapshot of a match currently in progress or just completed.
 * Used for late-join / reconnect scenarios so that clients can obtain the current
 * authoritative state without replaying the entire STOMP event stream.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LiveMatchSnapshot(
        Long matchId,
        Long tournamentId,

        String homeTeamName,
        String awayTeamName,

        int homeScore,
        int awayScore,

        Integer currentMinute,
        LiveMatchPhase phase,

        int latestSequence,

        LiveMatchEventType latestEventType,
        Instant lastUpdated,
        java.util.List<LiveMatchEvent> commentaryHistory,
        com.aditya.worldcup.simulation.dto.MatchSimulationResponse finalResult
) {
}
