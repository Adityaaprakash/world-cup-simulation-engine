package com.aditya.worldcup.standings.controller;

import com.aditya.worldcup.standings.dto.GroupStandingsResponse;
import com.aditya.worldcup.standings.service.StandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Standings", description = "Tournament group standings")
public class StandingController {

    private final StandingService standingService;

    @GetMapping("/{id}/standings")
    @Operation(summary = "Get standings", description = "Returns group standings for a tournament.")
    @ApiResponse(responseCode = "200", description = "Standings returned")
    public List<GroupStandingsResponse> getStandings(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return standingService.getStandings(id);
    }
}
