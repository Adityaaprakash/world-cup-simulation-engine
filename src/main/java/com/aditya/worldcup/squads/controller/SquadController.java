package com.aditya.worldcup.squads.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.squadplayers.dto.AddPlayerRequest;
import com.aditya.worldcup.squadplayers.dto.StartingXiRequest;
import com.aditya.worldcup.squadplayers.service.SquadPlayerService;
import com.aditya.worldcup.squads.dto.CreateSquadRequest;
import com.aditya.worldcup.squads.dto.SquadResponse;
import com.aditya.worldcup.squads.service.SquadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.aditya.worldcup.squadplayers.dto.CaptainRequest;
import com.aditya.worldcup.squadplayers.dto.PositionAssignmentRequest;
import com.aditya.worldcup.squadplayers.dto.LineupPlayerResponse;
import com.aditya.worldcup.squadplayers.dto.LineupValidationResponse;
import com.aditya.worldcup.squadplayers.dto.SquadReadyResponse;
import com.aditya.worldcup.simulation.dto.TeamStrengthResponse;
import com.aditya.worldcup.simulation.service.TeamStrengthService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
@Validated
@Tag(name = "Squads", description = "User squad, lineup, captain, and readiness operations")
public class SquadController {

    private final SquadService squadService;
    private final SquadPlayerService squadPlayerService;
    private final TeamStrengthService teamStrengthService;

    @PostMapping
    @Operation(summary = "Create squad", description = "Creates a user-owned squad for a national team and formation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Squad created"),
            @ApiResponse(responseCode = "400", description = "Invalid squad payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<SquadResponse> createSquad(
            @Valid @RequestBody CreateSquadRequest request,
            Authentication authentication
    ) {
        SquadResponse response = squadService.createSquad(
                request,
                authentication
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "List my squads", description = "Returns squads owned by the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Squads returned")
    public List<SquadResponse> getMySquads(
            Authentication authentication
    ) {
        return squadService.getMySquads(authentication);
    }

    @PostMapping("/{squadId}/players")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add squad player", description = "Adds an eligible player to a user-owned squad.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Player added"),
            @ApiResponse(responseCode = "400", description = "Invalid player payload"),
            @ApiResponse(responseCode = "403", description = "Squad belongs to another user"),
            @ApiResponse(responseCode = "409", description = "Squad is full or player already exists")
    })
    public void addPlayer(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId,
            @Valid @RequestBody AddPlayerRequest request,
            Authentication authentication
    ) {
        squadPlayerService.addPlayer(
                squadId,
                request,
                authentication
        );
    }

    @GetMapping("/{squadId}/players")
    @Operation(summary = "List squad players", description = "Returns players currently in a squad.")
    @ApiResponse(responseCode = "200", description = "Squad players returned")
    public List<PlayerResponse> getSquadPlayers(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId
    ) {
        return squadPlayerService.getSquadPlayers(squadId);
    }

    @DeleteMapping("/{squadId}/players/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove squad player", description = "Removes a player from a user-owned squad.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Player removed"),
            @ApiResponse(responseCode = "403", description = "Squad belongs to another user"),
            @ApiResponse(responseCode = "404", description = "Player not found in squad")
    })
    public void removePlayer(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId,
            @Parameter(description = "Player id")
            @PathVariable @Positive Long playerId,
            Authentication authentication
    ) {
        squadPlayerService.removePlayer(
                squadId,
                playerId,
                authentication
        );
    }

    @PutMapping("/{squadId}/starting-xi")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set starting XI", description = "Replaces the squad starting XI after validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Starting XI updated"),
            @ApiResponse(responseCode = "400", description = "Invalid starting XI"),
            @ApiResponse(responseCode = "403", description = "Squad belongs to another user"),
            @ApiResponse(responseCode = "409", description = "Unavailable player selected")
    })
    public void setStartingXi(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId,
            @Valid @RequestBody StartingXiRequest request,
            Authentication authentication
    ) {
        squadPlayerService.setStartingXi(
                squadId,
                request,
                authentication
        );
    }

    @PutMapping("/{squadId}/captain")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set captain", description = "Selects a captain from the squad starting XI.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Captain updated"),
            @ApiResponse(responseCode = "400", description = "Invalid captain selection"),
            @ApiResponse(responseCode = "403", description = "Squad belongs to another user")
    })
    public void setCaptain(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId,
            @Valid @RequestBody CaptainRequest request,
            Authentication authentication
    ) {

        squadPlayerService.setCaptain(
                squadId,
                request,
                authentication
        );
    }

    @PutMapping("/{squadId}/positions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Assign player position", description = "Assigns a formation position slot to a squad player.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Position assigned"),
            @ApiResponse(responseCode = "400", description = "Invalid position assignment"),
            @ApiResponse(responseCode = "403", description = "Squad belongs to another user")
    })
    public void assignPosition(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId,
            @Valid @RequestBody PositionAssignmentRequest request,
            Authentication authentication
    ) {

        squadPlayerService.assignPosition(
                squadId,
                request,
                authentication
        );
    }

    @GetMapping("/{squadId}/lineup")
    @Operation(summary = "Get lineup", description = "Returns squad lineup metadata including position slots and captain flags.")
    @ApiResponse(responseCode = "200", description = "Lineup returned")
    public List<LineupPlayerResponse> getLineup(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId
    ) {

        return squadPlayerService.getLineup(
                squadId
        );
    }

    @GetMapping("/{squadId}/validate")
    @Operation(summary = "Validate lineup", description = "Validates the current lineup against the squad formation.")
    @ApiResponse(responseCode = "200", description = "Lineup validation returned")
    public LineupValidationResponse validateLineup(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId
    ) {

        return squadPlayerService.validateLineup(
                squadId
        );
    }

    @GetMapping("/{squadId}/ready")
    @Operation(summary = "Get squad readiness", description = "Returns whether the squad is ready for match simulation.")
    @ApiResponse(responseCode = "200", description = "Squad readiness returned")
    public SquadReadyResponse getSquadReadyStatus(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId
    ) {

        return squadPlayerService.getSquadReadyStatus(
                squadId
        );
    }

    @GetMapping("/{squadId}/strength")
    @Operation(summary = "Get squad strength", description = "Returns calculated squad strength using the simulation rating logic.")
    @ApiResponse(responseCode = "200", description = "Squad strength returned")
    public TeamStrengthResponse getStrength(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId
    ) {

        return teamStrengthService.calculateStrength(
                squadId
        );
    }

    @GetMapping("/{squadId}/starting-xi")
    @Operation(summary = "Get starting XI", description = "Returns the current starting XI for a squad.")
    @ApiResponse(responseCode = "200", description = "Starting XI returned")
    public List<PlayerResponse> getStartingXi(
            @Parameter(description = "Squad id")
            @PathVariable @Positive Long squadId
    ) {
        return squadPlayerService.getStartingXi(squadId);
    }
}
