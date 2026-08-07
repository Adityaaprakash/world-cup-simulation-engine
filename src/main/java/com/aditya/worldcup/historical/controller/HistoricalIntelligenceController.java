package com.aditya.worldcup.historical.controller;

import com.aditya.worldcup.historical.dto.*;
import com.aditya.worldcup.historical.service.HistoricalIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoricalIntelligenceController {

    private final HistoricalIntelligenceService historicalIntelligenceService;

    @GetMapping("/players")
    public Page<PlayerLegacyResponse> players(@RequestParam(required = false) String name, Pageable pageable) { return historicalIntelligenceService.players(name, pageable); }
    @GetMapping("/teams")
    public Page<TeamLegacyResponse> teams(@RequestParam(required = false) String name, Pageable pageable) { return historicalIntelligenceService.teams(name, pageable); }
    @GetMapping("/managers")
    public Page<ManagerLegacyResponse> managers(@RequestParam(required = false) String name, Pageable pageable) { return historicalIntelligenceService.managers(name, pageable); }
    @GetMapping("/hall-of-fame")
    public HallOfFameResponse hallOfFame() { return historicalIntelligenceService.hallOfFame(); }
    @GetMapping("/rivalries")
    public List<RivalryResponse> rivalries(@RequestParam(required = false, defaultValue = "TEAM") String type) { return historicalIntelligenceService.rivalries(type); }
    @GetMapping("/head-to-head")
    public HeadToHeadResponse headToHead(@RequestParam(required = false, defaultValue = "TEAM") String type, @RequestParam Long firstId, @RequestParam Long secondId) { return historicalIntelligenceService.headToHead(type, firstId, secondId); }
    @GetMapping("/eras")
    public EraAnalysisResponse eras() { return historicalIntelligenceService.eras(); }
    @GetMapping("/timeline")
    public HistoricalTimelineResponse timeline() { return historicalIntelligenceService.timeline(); }
    @GetMapping("/rankings")
    public GlobalRankingResponse rankings() { return historicalIntelligenceService.rankings(); }
    @GetMapping("/summary")
    public HistoricalSummaryResponse summary() { return historicalIntelligenceService.summary(); }
}
