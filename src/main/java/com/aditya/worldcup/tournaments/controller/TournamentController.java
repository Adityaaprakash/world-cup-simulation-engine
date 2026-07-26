package com.aditya.worldcup.tournaments.controller;

import com.aditya.worldcup.tournaments.dto.CreateTournamentRequest;
import com.aditya.worldcup.tournaments.dto.TournamentResponse;
import com.aditya.worldcup.tournaments.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tournaments", description = "Tournament lifecycle management")
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    @Operation(summary = "Create tournament", description = "Creates a new upcoming tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tournament created"),
            @ApiResponse(responseCode = "400", description = "Invalid tournament payload")
    })
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody
            CreateTournamentRequest request) {

        TournamentResponse response = tournamentService.createTournament(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List tournaments", description = "Returns all tournaments using the legacy list response.")
    @ApiResponse(responseCode = "200", description = "Tournaments returned")
    public List<TournamentResponse> getAllTournaments() {

        return tournamentService.getAllTournaments();
    }

    @GetMapping("/page")
    @Operation(summary = "List tournaments with pagination", description = "Returns tournaments as a pageable response with optional sorting.")
    @ApiResponse(responseCode = "200", description = "Tournament page returned")
    public Page<TournamentResponse> getTournamentPage(
            @Parameter(description = "Pagination and sorting options")
            Pageable pageable) {

        return tournamentService.getTournamentPage(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament", description = "Returns a tournament by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament returned"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public TournamentResponse getTournament(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return tournamentService.getTournament(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete tournament", description = "Deletes an upcoming tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tournament deleted"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Tournament cannot be deleted in its current state")
    })
    public void deleteTournament(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        tournamentService.deleteTournament(id);
    }
}
