package com.aditya.worldcup.squadplayers.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.squadplayers.dto.AddPlayerRequest;
import com.aditya.worldcup.squadplayers.service.SquadPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class SquadPlayerController {

    private final SquadPlayerService squadPlayerService;

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
}