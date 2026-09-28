package com.aditya.worldcup.managers.dto;

import com.aditya.worldcup.managers.entity.ObjectiveStatus;
import com.aditya.worldcup.managers.entity.ObjectiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerObjectiveResponse {
    private Long id;
    private ObjectiveType type;
    private String description;
    private Integer targetValue;
    private Integer currentValue;
    private ObjectiveStatus status;
    private Integer rewardAmount;
    private Long tournamentId;
    private String tournamentName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
