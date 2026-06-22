package com.aditya.worldcup.matchevents.dto;

public record MatchEventResponse(
        Integer minute,
        String player,
        String eventType,
        String description
) {
}