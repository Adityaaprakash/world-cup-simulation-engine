package com.aditya.worldcup.players.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Validated
@Tag(name = "Players", description = "Player catalog")
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    @Operation(summary = "List players", description = "Returns all players using the legacy list response.")
    @ApiResponse(responseCode = "200", description = "Players returned")
    public List<PlayerResponse> getAllPlayers() {

        return playerService.getAllPlayers();
    }

    @GetMapping("/page")
    @Operation(summary = "List players with pagination", description = "Returns players as a pageable response with optional sorting.")
    @ApiResponse(responseCode = "200", description = "Player page returned")
    public Page<PlayerResponse> getPlayerPage(
            @Parameter(description = "Pagination and sorting options")
            Pageable pageable) {

        return playerService.getPlayerPage(pageable);
    }

    @GetMapping("/country/{countryId}")
    @Operation(summary = "List players by country", description = "Returns players whose country matches the provided country id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Players returned"),
            @ApiResponse(responseCode = "400", description = "Invalid country id")
    })
    public List<PlayerResponse> getPlayersByCountry(
            @Parameter(description = "Country id")
            @PathVariable @Positive Long countryId
    ) {

        return playerService.getPlayersByCountry(countryId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get player details", description = "Returns detailed player attributes and current form/fitness")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Player details returned"),
            @ApiResponse(responseCode = "404", description = "Player not found")
    })
    public com.aditya.worldcup.players.dto.PlayerDetailsResponse getPlayerDetails(
            @Parameter(description = "Player id")
            @PathVariable @Positive Long id
    ) {
        return playerService.getPlayerDetails(id);
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare players", description = "Returns details for multiple players to compare them side-by-side")
    @ApiResponse(responseCode = "200", description = "Player comparison returned")
    public List<com.aditya.worldcup.players.dto.PlayerDetailsResponse> comparePlayers(
            @jakarta.validation.Valid @RequestBody com.aditya.worldcup.players.dto.ComparePlayersRequest request
    ) {
        return playerService.comparePlayers(request.playerIds());
    }
}
