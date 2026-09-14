package com.aditya.worldcup.training.controller;

import com.aditya.worldcup.training.dto.TrainingRequest;
import com.aditya.worldcup.training.service.PlayerTrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class PlayerTrainingController {

    private final PlayerTrainingService playerTrainingService;

    @PostMapping("/squads/{squadId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> trainSquad(
            @PathVariable Long squadId,
            @Valid @RequestBody TrainingRequest request
    ) {
        playerTrainingService.trainSquad(squadId, request.category(), request.intensity());

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Squad trained successfully with " + request.intensity() + " intensity on " + request.category()
        ));
    }
}
