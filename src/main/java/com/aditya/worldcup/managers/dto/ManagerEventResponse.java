package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.ManagerEventStatus;
import com.aditya.worldcup.managers.entity.ManagerEventType;
import java.time.LocalDateTime;
import java.util.List;

public record ManagerEventResponse(
        Long id,
        ManagerEventType type,
        String title,
        String description,
        ManagerEventStatus status,
        String selectedDecision,
        String resolutionText,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime resolvedAt,
        Long relatedPlayerId,
        Long relatedMatchId,
        Long relatedTournamentId,
        List<DecisionOptionResponse> availableDecisions
) {}
