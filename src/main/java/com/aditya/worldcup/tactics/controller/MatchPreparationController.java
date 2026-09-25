package com.aditya.worldcup.tactics.controller;

import com.aditya.worldcup.tactics.dto.MatchPlanDto;
import com.aditya.worldcup.tactics.dto.MatchPreparationResponse;
import com.aditya.worldcup.tactics.entity.MatchPlan;
import com.aditya.worldcup.tactics.service.MatchPlanService;
import com.aditya.worldcup.tactics.service.OpponentAnalysisService;
import com.aditya.worldcup.saves.context.SaveContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/preparation")
@RequiredArgsConstructor
public class MatchPreparationController {

    private final MatchPlanService matchPlanService;
    private final OpponentAnalysisService opponentAnalysisService;

    @GetMapping
    public ResponseEntity<MatchPreparationResponse> getMatchPreparation(
            @PathVariable Long matchId,
            @RequestParam Long squadId) {
        Long managerId = SaveContextHolder.getManagerId();
        MatchPreparationResponse response = opponentAnalysisService.analyzePreparation(matchId, squadId, managerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<MatchPlanDto> saveMatchPlan(
            @PathVariable Long matchId,
            @RequestBody MatchPlanDto request) {
        Long managerId = SaveContextHolder.getManagerId();
        MatchPlan saved = matchPlanService.saveMatchPlan(matchId, managerId, request);
        return ResponseEntity.ok(matchPlanService.mapToDto(saved));
    }
}
