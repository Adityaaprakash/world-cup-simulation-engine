package com.aditya.worldcup.search.controller;

import com.aditya.worldcup.managers.dto.ManagerResponse;
import com.aditya.worldcup.matches.dto.MatchResponse;
import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.search.dto.ManagerSearchRequest;
import com.aditya.worldcup.search.dto.MatchSearchRequest;
import com.aditya.worldcup.search.dto.PlayerSearchRequest;
import com.aditya.worldcup.search.dto.SearchResultResponse;
import com.aditya.worldcup.search.dto.TeamSearchRequest;
import com.aditya.worldcup.search.dto.TournamentSearchRequest;
import com.aditya.worldcup.search.service.ManagerSearchService;
import com.aditya.worldcup.search.service.MatchSearchService;
import com.aditya.worldcup.search.service.PlayerSearchService;
import com.aditya.worldcup.search.service.TeamSearchService;
import com.aditya.worldcup.search.service.TournamentSearchService;
import com.aditya.worldcup.teams.dto.TeamResponse;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Search", description = "Advanced paginated search across football and career data")
public class SearchController {

    private final PlayerSearchService playerSearchService;
    private final TeamSearchService teamSearchService;
    private final ManagerSearchService managerSearchService;
    private final TournamentSearchService tournamentSearchService;
    private final MatchSearchService matchSearchService;

    @PostMapping("/players")
    @Operation(summary = "Search players")
    public SearchResultResponse<PlayerResponse> players(
            @RequestBody(required = false) PlayerSearchRequest request) {
        return playerSearchService.search(request);
    }

    @PostMapping("/teams")
    @Operation(summary = "Search national teams")
    public SearchResultResponse<TeamResponse> teams(
            @RequestBody(required = false) TeamSearchRequest request) {
        return teamSearchService.search(request);
    }

    @PostMapping("/managers")
    @Operation(summary = "Search managers and career performance")
    public SearchResultResponse<ManagerResponse> managers(
            @RequestBody(required = false) ManagerSearchRequest request) {
        return managerSearchService.search(request);
    }

    @PostMapping("/tournaments")
    @Operation(summary = "Search tournaments")
    public SearchResultResponse<TournamentResponse> tournaments(
            @RequestBody(required = false) TournamentSearchRequest request) {
        return tournamentSearchService.search(request);
    }

    @PostMapping("/matches")
    @Operation(summary = "Search matches")
    public SearchResultResponse<MatchResponse> matches(
            @RequestBody(required = false) MatchSearchRequest request) {
        return matchSearchService.search(request);
    }
}
