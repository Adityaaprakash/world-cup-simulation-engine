package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.BoardObjective;
import jakarta.validation.constraints.NotNull;

public record ManagerJobRequest(
        @NotNull(message = "Team ID is required")
        Long teamId,
        
        @NotNull(message = "Target objective is required")
        BoardObjective targetObjective
) {}
