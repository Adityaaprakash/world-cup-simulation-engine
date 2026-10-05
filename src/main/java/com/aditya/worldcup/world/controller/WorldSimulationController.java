package com.aditya.worldcup.world.controller;

import com.aditya.worldcup.saves.dto.SaveSlotResponse;
import com.aditya.worldcup.saves.entity.SaveSlot;
import com.aditya.worldcup.saves.service.SaveGameService;
import com.aditya.worldcup.world.service.WorldSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/world")
@RequiredArgsConstructor
@Tag(name = "World Simulation", description = "Endpoints for dynamic world evolution and progression")
public class WorldSimulationController {

    private final WorldSimulationService worldSimulationService;
    private final SaveGameService saveGameService;

    @PostMapping("/advance")
    @Operation(summary = "Advance the world by one season")
    public ResponseEntity<SaveSlotResponse> advanceWorld(Authentication authentication) {
        SaveSlot activeSlot = worldSimulationService.advanceWorldTick(authentication);
        return ResponseEntity.ok(saveGameService.toResponse(activeSlot));
    }
}
