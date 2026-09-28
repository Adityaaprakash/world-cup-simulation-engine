package com.aditya.worldcup.managers.controller;

import com.aditya.worldcup.managers.dto.CareerHistoryResponse;
import com.aditya.worldcup.managers.dto.CareerTimelineResponse;
import com.aditya.worldcup.managers.dto.ManagerObjectiveResponse;
import com.aditya.worldcup.managers.entity.ManagerObjective;
import com.aditya.worldcup.managers.service.CareerHistoryService;
import com.aditya.worldcup.managers.service.CareerTimelineService;
import com.aditya.worldcup.managers.service.ManagerObjectiveService;
import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.saves.context.SaveContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/career")
@RequiredArgsConstructor
public class ManagerCareerController {

    private final ManagerObjectiveService managerObjectiveService;
    private final CareerHistoryService careerHistoryService;
    private final CareerTimelineService careerTimelineService;
    private final ManagerService managerService;

    @GetMapping("/objectives")
    public ResponseEntity<List<ManagerObjectiveResponse>> getObjectives() {
        Long managerId = SaveContextHolder.getManagerId();
        return ResponseEntity.ok(managerObjectiveService.getActiveObjectives(managerId).stream().map(this::mapToDto).toList());
    }

    @GetMapping("/objectives/all")
    public ResponseEntity<List<ManagerObjectiveResponse>> getAllObjectives() {
        Long managerId = SaveContextHolder.getManagerId();
        return ResponseEntity.ok(managerObjectiveService.getAllObjectives(managerId).stream().map(this::mapToDto).toList());
    }

    @GetMapping("/history")
    public ResponseEntity<List<CareerHistoryResponse>> getCareerHistory(Authentication authentication) {
        return ResponseEntity.ok(careerHistoryService.getCurrentHistory(authentication));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<CareerTimelineResponse>> getCareerTimeline(Authentication authentication) {
        return ResponseEntity.ok(careerTimelineService.getCurrentTimeline(authentication));
    }

    private ManagerObjectiveResponse mapToDto(ManagerObjective obj) {
        return ManagerObjectiveResponse.builder()
                .id(obj.getId())
                .type(obj.getType())
                .description(obj.getDescription())
                .targetValue(obj.getTargetValue())
                .currentValue(obj.getCurrentValue())
                .status(obj.getStatus())
                .rewardAmount(obj.getRewardAmount())
                .tournamentId(obj.getTournament() != null ? obj.getTournament().getId() : null)
                .tournamentName(obj.getTournament() != null ? obj.getTournament().getName() : null)
                .createdAt(obj.getCreatedAt())
                .completedAt(obj.getCompletedAt())
                .build();
    }
}
