package com.aditya.worldcup.scouting.controller;

import com.aditya.worldcup.managers.service.ManagerService;
import com.aditya.worldcup.scouting.dto.ScoutAssignmentRequest;
import com.aditya.worldcup.scouting.dto.ScoutDto;
import com.aditya.worldcup.scouting.dto.ScoutingReportDto;
import com.aditya.worldcup.scouting.entity.Scout;
import com.aditya.worldcup.scouting.entity.ScoutingReport;
import com.aditya.worldcup.scouting.repository.ScoutingReportRepository;
import com.aditya.worldcup.scouting.service.ScoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scouting")
@RequiredArgsConstructor
public class ScoutingController {

    private final ScoutingService scoutingService;
    private final ScoutingReportRepository reportRepository;
    private final ManagerService managerService;

    @GetMapping("/scouts")
    public ResponseEntity<List<ScoutDto>> getScouts() {
        List<ScoutDto> dtos = scoutingService.getScoutsForCurrentManager().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/assignments")
    public ResponseEntity<ScoutingReportDto> assignScout(@RequestBody ScoutAssignmentRequest request) {
        ScoutingReport report = scoutingService.assignScout(request.getScoutId(), request.getPlayerId());
        return ResponseEntity.ok(mapToDto(report));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ScoutingReportDto>> getReports() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        List<ScoutingReportDto> dtos = reportRepository.findByManagerOrderByCreatedAtDesc(managerService.getOrCreateManager(auth))
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/simulate-advance")
    public ResponseEntity<Void> simulateAdvance(@RequestParam(defaultValue = "1") int days) {
        scoutingService.advanceAllAssignments(days);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/assignments/{id}/complete")
    public ResponseEntity<Void> completeAssignment(@PathVariable Long id) {
        scoutingService.forceCompleteAssignment(id);
        return ResponseEntity.ok().build();
    }

    private ScoutDto mapToDto(Scout scout) {
        return ScoutDto.builder()
                .id(scout.getId())
                .name(scout.getName())
                .evaluationSkill(scout.getEvaluationSkill())
                .potentialEvaluationSkill(scout.getPotentialEvaluationSkill())
                .tacticalKnowledge(scout.getTacticalKnowledge())
                .regionSpecialization(scout.getRegionSpecialization())
                .build();
    }

    private ScoutingReportDto mapToDto(ScoutingReport report) {
        return ScoutingReportDto.builder()
                .id(report.getId())
                .scoutId(report.getScout().getId())
                .scoutName(report.getScout().getName())
                .playerId(report.getPlayer().getId())
                .playerName(report.getPlayer().getName())
                .status(report.getStatus().name())
                .progress(report.getProgress())
                .knowledgeLevel(report.getKnowledgeLevel().name())
                .estimatedOverallMin(report.getEstimatedOverallMin())
                .estimatedOverallMax(report.getEstimatedOverallMax())
                .estimatedPotentialMin(report.getEstimatedPotentialMin())
                .estimatedPotentialMax(report.getEstimatedPotentialMax())
                .confidence(report.getConfidence())
                .tacticalCompatibility(report.getTacticalCompatibility())
                .recommendation(report.getRecommendation() != null ? report.getRecommendation().name() : null)
                .createdAt(report.getCreatedAt())
                .completedAt(report.getCompletedAt())
                .build();
    }
}
