package com.aditya.worldcup.standings.controller;

import com.aditya.worldcup.standings.dto.GroupStandingsResponse;
import com.aditya.worldcup.standings.service.StandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class StandingController {

    private final StandingService standingService;

    @GetMapping("/{id}/standings")
    public List<GroupStandingsResponse> getStandings(
            @PathVariable Long id) {

        return standingService.getStandings(id);
    }
}
