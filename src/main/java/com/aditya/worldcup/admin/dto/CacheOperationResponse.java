package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;

public record CacheOperationResponse(
        String operation,
        String cache,
        String status,
        long durationMs,
        String details,
        LocalDateTime completedAt
) {
}
