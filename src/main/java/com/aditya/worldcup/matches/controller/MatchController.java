package com.aditya.worldcup.matches.controller;

import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.matches.dto.MatchResultRequest;
import com.aditya.worldcup.matches.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/{id}/matches")
    public List<MatchResponse> getTournamentMatches(
            @PathVariable Long id) {

        return matchService.getTournamentMatches(id);
    }

    @PostMapping("/{id}/matches/{matchId}/complete")
    public MatchResponse completeMatch(
            @PathVariable Long id,
            @PathVariable Long matchId,
            @RequestBody MatchResultRequest request
    ) {

        return matchService.completeMatch(
                id,
                matchId,
                request
        );
    }
}
