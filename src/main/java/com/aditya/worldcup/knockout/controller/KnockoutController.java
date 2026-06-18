package com.aditya.worldcup.knockout.controller;

import com.aditya.worldcup.knockout.dto.KnockoutBracketResponse;
import com.aditya.worldcup.knockout.service.KnockoutQualificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class KnockoutController {

    private final KnockoutQualificationService knockoutQualificationService;

    @PostMapping("/{id}/knockout/generate")
    public KnockoutBracketResponse generateKnockout(
            @PathVariable Long id) {

        return knockoutQualificationService.generateKnockout(id);
    }
}
