package com.aditya.worldcup.tournaments.controller;

import com.aditya.worldcup.optimization.service.CachedTournamentDataService;
import com.aditya.worldcup.tournaments.dto.TournamentAwardsResponse;
import com.aditya.worldcup.tournaments.dto.TournamentSummaryResponse;
import com.aditya.worldcup.tournaments.dto.TournamentTeamAwardsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentAwardsController {

    private final CachedTournamentDataService cachedTournamentDataService;

    @GetMapping("/{tournamentId}/awards")
    public TournamentAwardsResponse getAwards(
            @PathVariable Long tournamentId
    ) {

        return cachedTournamentDataService.getAwards(tournamentId);
    }

    @GetMapping("/{tournamentId}/team-awards")
    public TournamentTeamAwardsResponse getTeamAwards(
            @PathVariable Long tournamentId
    ) {

        return cachedTournamentDataService.getTeamAwards(tournamentId);
    }

    @GetMapping("/{tournamentId}/summary")
    public TournamentSummaryResponse getSummary(
            @PathVariable Long tournamentId
    ) {

        return cachedTournamentDataService.getSummary(tournamentId);
    }
}
