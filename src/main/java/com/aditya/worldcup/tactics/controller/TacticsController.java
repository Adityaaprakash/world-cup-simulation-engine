package com.aditya.worldcup.tactics.controller;

import com.aditya.worldcup.tactics.dto.TacticalProfileResponse;
import com.aditya.worldcup.tactics.dto.TacticalProfileUpdateRequest;
import com.aditya.worldcup.tactics.entity.TacticalProfile;
import com.aditya.worldcup.tactics.service.TacticalProfileService;
import com.aditya.worldcup.teams.entity.Team;
import com.aditya.worldcup.teams.repository.TeamRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/tactics")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tactics", description = "Team tactical profile configuration")
public class TacticsController {

    private final TacticalProfileService tacticalProfileService;
    private final TeamRepository teamRepository;

    @GetMapping
    @Operation(summary = "Get tactical profile", description = "Returns the overarching tactical profile for a team.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tactical profile returned"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<TacticalProfileResponse> getTacticalProfile(
            @Parameter(description = "Team id")
            @PathVariable @Positive Long teamId
    ) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        TacticalProfile profile = tacticalProfileService.getOrCreateProfile(team);
        return ResponseEntity.ok(mapToResponse(profile));
    }

    @PutMapping
    @Operation(summary = "Update tactical profile", description = "Updates the overarching tactical profile for a team.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tactical profile updated"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<TacticalProfileResponse> updateTacticalProfile(
            @Parameter(description = "Team id")
            @PathVariable @Positive Long teamId,
            @Valid @RequestBody TacticalProfileUpdateRequest request
    ) {
        TacticalProfile updated = tacticalProfileService.updateProfile(teamId, request);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    private TacticalProfileResponse mapToResponse(TacticalProfile profile) {
        return new TacticalProfileResponse(
                profile.getId(),
                profile.getTeam().getId(),
                profile.getPressingIntensity(),
                profile.getDefensiveLine(),
                profile.getTempo(),
                profile.getWidth(),
                profile.getPassingStyle(),
                profile.getAttackingApproach(),
                profile.getBuildUpStyle(),
                profile.getDefensiveBlock()
        );
    }
}
