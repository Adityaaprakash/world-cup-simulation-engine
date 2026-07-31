package com.aditya.worldcup.admin.controller;

import com.aditya.worldcup.admin.dto.AdminHealthResponse;
import com.aditya.worldcup.admin.dto.DashboardResponse;
import com.aditya.worldcup.admin.service.AdminDashboardService;
import com.aditya.worldcup.admin.service.AdminHealthService;
import com.aditya.worldcup.admin.service.AdminTournamentService;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrator monitoring and tournament management")
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminHealthService adminHealthService;
    private final AdminTournamentService adminTournamentService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard", description = "Returns a consolidated administrator dashboard with system, manager, tournament, career, save, and infrastructure metrics.")
    @ApiResponse(responseCode = "200", description = "Dashboard returned")
    public DashboardResponse dashboard() {
        return adminDashboardService.dashboard();
    }

    @GetMapping("/health")
    @Operation(summary = "Get admin health summary", description = "Returns summarized operational health for infrastructure and active workload.")
    @ApiResponse(responseCode = "200", description = "Health summary returned")
    public AdminHealthResponse health() {
        return adminHealthService.healthSummary();
    }

    @GetMapping("/tournaments")
    @Operation(summary = "List all tournaments", description = "Returns every tournament for administrator review.")
    @ApiResponse(responseCode = "200", description = "Tournaments returned")
    public List<TournamentResponse> listTournaments() {
        return adminTournamentService.listTournaments();
    }

    @PutMapping("/tournaments/{id}/archive")
    @Operation(summary = "Archive tournament", description = "Archives a completed tournament and records an admin audit event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament archived"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Tournament cannot be archived")
    })
    public TournamentResponse archiveTournament(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminTournamentService.archiveTournament(id, authentication);
    }

    @PutMapping("/tournaments/{id}/reopen")
    @Operation(summary = "Reopen archived tournament", description = "Restores an archived tournament to completed status and records an admin audit event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament reopened"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Tournament cannot be reopened")
    })
    public TournamentResponse reopenTournament(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminTournamentService.reopenTournament(id, authentication);
    }

    @PostMapping("/tournaments/{id}/reset")
    @Operation(summary = "Reset tournament", description = "Resets simulation state for a tournament through the existing tournament service and records an admin audit event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament reset"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Tournament cannot be reset")
    })
    public TournamentResponse resetTournament(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminTournamentService.resetTournament(id, authentication);
    }

    @DeleteMapping("/tournaments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete inactive tournament", description = "Deletes an inactive tournament and records an admin audit event.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tournament deleted"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Active or completed tournament cannot be deleted")
    })
    public void deleteTournament(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id,
            Authentication authentication) {

        adminTournamentService.deleteTournament(id, authentication);
    }
}
