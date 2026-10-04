package com.aditya.worldcup.scouting.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ScoutingReportDto {
    private Long id;
    private Long scoutId;
    private String scoutName;
    private Long playerId;
    private String playerName;
    
    private String status;
    private Integer progress;
    private String knowledgeLevel;
    
    // Will be null if knowledgeLevel == NONE
    private Integer estimatedOverallMin;
    private Integer estimatedOverallMax;
    private Integer estimatedPotentialMin;
    private Integer estimatedPotentialMax;
    
    private Integer confidence;
    private Integer tacticalCompatibility;
    private String recommendation;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
