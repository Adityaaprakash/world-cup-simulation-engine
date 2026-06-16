package com.aditya.worldcup.players.controller;

import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.players.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public List<PlayerResponse> getAllPlayers() {

        return playerService.getAllPlayers();
    }

    @GetMapping("/country/{countryId}")
    public List<PlayerResponse> getPlayersByCountry(
            @PathVariable Long countryId
    ) {

        return playerService.getPlayersByCountry(countryId);
    }
}