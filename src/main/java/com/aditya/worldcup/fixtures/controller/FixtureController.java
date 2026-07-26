package com.aditya.worldcup.fixtures.controller;

import com.aditya.worldcup.fixtures.dto.FixtureGenerationResponse;
import com.aditya.worldcup.fixtures.service.FixtureGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Fixtures", description = "Tournament fixture generation")
public class FixtureController {

    private final FixtureGenerationService fixtureGenerationService;

    @PostMapping("/{id}/fixtures/generate")
    @Operation(summary = "Generate group-stage fixtures", description = "Creates scheduled group-stage matches for generated tournament groups.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fixtures generated"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Fixtures already generated or tournament state invalid")
    })
    public FixtureGenerationResponse generateFixtures(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return fixtureGenerationService.generateFixtures(id);
    }
}
