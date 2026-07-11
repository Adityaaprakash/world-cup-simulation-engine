package com.aditya.worldcup.tournaments.controller;

import com.aditya.worldcup.tournaments.dto.TournamentAwardsResponse;
import com.aditya.worldcup.tournaments.service.TournamentAwardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentAwardsController {

    private final TournamentAwardsService tournamentAwardsService;

    @GetMapping("/{tournamentId}/awards")
    public TournamentAwardsResponse getAwards(
            @PathVariable Long tournamentId
    ) {

        return tournamentAwardsService.calculateAwards(tournamentId);
    }
}
