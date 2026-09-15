package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.BoardObjective;
import com.aditya.worldcup.managers.entity.JobStatus;
import java.time.LocalDateTime;

public record ManagerJobResponse(
        Long id,
        Long managerId,
        String managerName,
        Long teamId,
        String teamName,
        BoardObjective targetObjective,
        Double boardConfidence,
        JobStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
