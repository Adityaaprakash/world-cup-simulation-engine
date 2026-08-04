package com.aditya.worldcup.admin.controller;

import com.aditya.worldcup.admin.dto.AdminHealthResponse;
import com.aditya.worldcup.admin.dto.BulkPlayerUpdateRequest;
import com.aditya.worldcup.admin.dto.BulkPlayerUpdateResponse;
import com.aditya.worldcup.admin.dto.DashboardResponse;
import com.aditya.worldcup.admin.dto.DatasetHealthResponse;
import com.aditya.worldcup.admin.dto.PlayerUpdateRequest;
import com.aditya.worldcup.admin.dto.TeamRefreshResponse;
import com.aditya.worldcup.admin.dto.TeamUpdateRequest;
import com.aditya.worldcup.admin.service.AdminDashboardService;
import com.aditya.worldcup.admin.service.AdminHealthService;
import com.aditya.worldcup.admin.service.AdminPlayerService;
import com.aditya.worldcup.admin.service.AdminTeamService;
import com.aditya.worldcup.admin.service.AdminTournamentService;
import com.aditya.worldcup.admin.service.DatasetHealthService;
import com.aditya.worldcup.players.dto.PlayerResponse;
import com.aditya.worldcup.teams.dto.TeamResponse;
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
    private final AdminPlayerService adminPlayerService;
    private final AdminTeamService adminTeamService;
    private final DatasetHealthService datasetHealthService;

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

    @GetMapping("/dataset/health")
    @Operation(summary = "Get football dataset health", description = "Returns player and team status totals plus dataset validation findings.")
    public DatasetHealthResponse datasetHealth() {
        return datasetHealthService.health();
    }

    @PutMapping("/players/{id}")
    @Operation(summary = "Update player ratings", description = "Updates one or more FIFA-style player ratings.")
    public PlayerResponse updatePlayer(
            @PathVariable @Positive Long id,
            @RequestBody PlayerUpdateRequest request,
            Authentication authentication) {

        return adminPlayerService.updatePlayer(id, request, authentication);
    }

    @PostMapping("/players/bulk-update")
    @Operation(summary = "Bulk update player ratings", description = "Processes every update and returns validation failures without abandoning valid updates.")
    public BulkPlayerUpdateResponse bulkUpdatePlayers(
            @RequestBody BulkPlayerUpdateRequest request,
            Authentication authentication) {

        return adminPlayerService.bulkUpdate(request, authentication);
    }

    @PutMapping("/players/{id}/activate")
    @Operation(summary = "Activate player")
    public PlayerResponse activatePlayer(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminPlayerService.activatePlayer(id, authentication);
    }

    @PutMapping("/players/{id}/deactivate")
    @Operation(summary = "Deactivate player")
    public PlayerResponse deactivatePlayer(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminPlayerService.deactivatePlayer(id, authentication);
    }

    @PutMapping("/players/{id}/retire")
    @Operation(summary = "Retire player")
    public PlayerResponse retirePlayer(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminPlayerService.retirePlayer(id, authentication);
    }

    @PutMapping("/players/{id}/restore")
    @Operation(summary = "Restore retired player")
    public PlayerResponse restorePlayer(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminPlayerService.restorePlayer(id, authentication);
    }

    @PutMapping("/teams/{id}")
    @Operation(summary = "Update national team data", description = "Updates FIFA ranking, confederation, or manager details.")
    public TeamResponse updateTeam(
            @PathVariable @Positive Long id,
            @RequestBody TeamUpdateRequest request,
            Authentication authentication) {

        return adminTeamService.updateTeam(id, request, authentication);
    }

    @PutMapping("/teams/{id}/activate")
    @Operation(summary = "Activate national team")
    public TeamResponse activateTeam(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminTeamService.activateTeam(id, authentication);
    }

    @PutMapping("/teams/{id}/deactivate")
    @Operation(summary = "Deactivate national team")
    public TeamResponse deactivateTeam(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminTeamService.deactivateTeam(id, authentication);
    }

    @PostMapping("/teams/{id}/refresh-squad")
    @Operation(summary = "Refresh national team squad", description = "Recalculates the team rating from active national players and reports squad validation findings.")
    public TeamRefreshResponse refreshTeamSquad(
            @PathVariable @Positive Long id,
            Authentication authentication) {

        return adminTeamService.refreshSquad(id, authentication);
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
