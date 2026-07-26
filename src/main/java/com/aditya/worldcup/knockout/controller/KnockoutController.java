package com.aditya.worldcup.knockout.controller;

import com.aditya.worldcup.knockout.dto.KnockoutBracketResponse;
import com.aditya.worldcup.knockout.service.KnockoutQualificationService;
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
@Tag(name = "Knockout", description = "Knockout bracket generation")
public class KnockoutController {

    private final KnockoutQualificationService knockoutQualificationService;

    @PostMapping("/{id}/knockout/generate")
    @Operation(summary = "Generate knockout bracket", description = "Creates round-of-16 knockout fixtures after the group stage is complete.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Knockout bracket generated"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Knockout cannot be generated yet or is already generated")
    })
    public KnockoutBracketResponse generateKnockout(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return knockoutQualificationService.generateKnockout(id);
    }
}
