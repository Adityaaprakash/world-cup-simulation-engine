package com.aditya.worldcup.fixtures.controller;

import com.aditya.worldcup.fixtures.dto.FixtureGenerationResponse;
import com.aditya.worldcup.fixtures.service.FixtureGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class FixtureController {

    private final FixtureGenerationService fixtureGenerationService;

    @PostMapping("/{id}/fixtures/generate")
    public FixtureGenerationResponse generateFixtures(
            @PathVariable Long id) {

        return fixtureGenerationService.generateFixtures(id);
    }
}
