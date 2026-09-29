package com.aditya.worldcup.matchevents.dto;

public record MatchEventResponse(
        Integer minute,
        String player,
        Long playerId,
        String teamName,
        Long teamId,
        String eventType,
        String description
) {
    public MatchEventResponse(Integer minute, String player, String eventType, String description) {
        this(minute, player, null, null, null, eventType, description);
    }
}