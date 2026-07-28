package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.TimelineEventType;

import java.time.LocalDateTime;

public record CareerTimelineResponse(
        Long id,
        TimelineEventType eventType,
        String title,
        String description,
        Long tournamentId,
        Long teamId,
        LocalDateTime occurredAt
) {
}
