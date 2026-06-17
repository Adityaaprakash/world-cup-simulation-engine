package com.aditya.worldcup.squads.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.squadplayers.dto.AddPlayerRequest;
import com.aditya.worldcup.squadplayers.dto.StartingXiRequest;
import com.aditya.worldcup.squadplayers.service.SquadPlayerService;
import com.aditya.worldcup.squads.dto.CreateSquadRequest;
import com.aditya.worldcup.squads.dto.SquadResponse;
import com.aditya.worldcup.squads.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.aditya.worldcup.squadplayers.dto.CaptainRequest;
import com.aditya.worldcup.squadplayers.dto.PositionAssignmentRequest;
import com.aditya.worldcup.squadplayers.dto.LineupPlayerResponse;
import com.aditya.worldcup.squadplayers.dto.LineupValidationResponse;
import com.aditya.worldcup.squadplayers.dto.SquadReadyResponse;

import java.util.List;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;
    private final SquadPlayerService squadPlayerService;

    @PostMapping
    public SquadResponse createSquad(
            @RequestBody CreateSquadRequest request,
            Authentication authentication
    ) {
        return squadService.createSquad(
                request,
                authentication
        );
    }

    @GetMapping("/my")
    public List<SquadResponse> getMySquads(
            Authentication authentication
    ) {
        return squadService.getMySquads(authentication);
    }

    @PostMapping("/{squadId}/players")
    public void addPlayer(
            @PathVariable Long squadId,
            @RequestBody AddPlayerRequest request,
            Authentication authentication
    ) {
        squadPlayerService.addPlayer(
                squadId,
                request,
                authentication
        );
    }

    @GetMapping("/{squadId}/players")
    public List<PlayerResponse> getSquadPlayers(
            @PathVariable Long squadId
    ) {
        return squadPlayerService.getSquadPlayers(squadId);
    }

    @DeleteMapping("/{squadId}/players/{playerId}")
    public void removePlayer(
            @PathVariable Long squadId,
            @PathVariable Long playerId,
            Authentication authentication
    ) {
        squadPlayerService.removePlayer(
                squadId,
                playerId,
                authentication
        );
    }

    @PutMapping("/{squadId}/starting-xi")
    public void setStartingXi(
            @PathVariable Long squadId,
            @RequestBody StartingXiRequest request,
            Authentication authentication
    ) {
        squadPlayerService.setStartingXi(
                squadId,
                request,
                authentication
        );
    }

    @PutMapping("/{squadId}/captain")
    public void setCaptain(
            @PathVariable Long squadId,
            @RequestBody CaptainRequest request,
            Authentication authentication
    ) {

        squadPlayerService.setCaptain(
                squadId,
                request,
                authentication
        );
    }

    @PutMapping("/{squadId}/positions")
    public void assignPosition(
            @PathVariable Long squadId,
            @RequestBody PositionAssignmentRequest request,
            Authentication authentication
    ) {

        squadPlayerService.assignPosition(
                squadId,
                request,
                authentication
        );
    }

    @GetMapping("/{squadId}/lineup")
    public List<LineupPlayerResponse> getLineup(
            @PathVariable Long squadId
    ) {

        return squadPlayerService.getLineup(
                squadId
        );
    }

    @GetMapping("/{squadId}/validate")
    public LineupValidationResponse validateLineup(
            @PathVariable Long squadId
    ) {

        return squadPlayerService.validateLineup(
                squadId
        );
    }

    @GetMapping("/{squadId}/ready")
    public SquadReadyResponse getSquadReadyStatus(
            @PathVariable Long squadId
    ) {

        return squadPlayerService.getSquadReadyStatus(
                squadId
        );
    }

    @GetMapping("/{squadId}/starting-xi")
    public List<PlayerResponse> getStartingXi(
            @PathVariable Long squadId
    ) {
        return squadPlayerService.getStartingXi(squadId);
    }
}