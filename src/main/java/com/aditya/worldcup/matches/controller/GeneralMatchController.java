package com.aditya.worldcup.matches.controller;

import com.aditya.worldcup.matches.dto.MatchDetailResponse;
import com.aditya.worldcup.matches.dto.MatchHistoryResponse;
import com.aditya.worldcup.matches.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class GeneralMatchController {

    private final MatchService matchService;

    @GetMapping("/history")
    public List<MatchHistoryResponse> getMatchHistory() {
        return matchService.getMatchHistory();
    }

    @GetMapping("/{matchId}")
    public MatchDetailResponse getMatchDetail(
            @PathVariable Long matchId
    ) {
        return matchService.getMatchDetail(matchId);
    }
}
