package com.aditya.worldcup.admin.dto;

import java.time.LocalDateTime;

public record MaintenanceHistoryResponse(
        Long id,
        String operation,
        String administrator,
        Long durationMs,
        String status,
        String details,
        LocalDateTime createdAt
) {
}
