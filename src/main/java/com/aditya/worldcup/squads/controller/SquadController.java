package com.aditya.worldcup.squads.controller;

import com.aditya.worldcup.squads.dto.CreateSquadRequest;
import com.aditya.worldcup.squads.dto.SquadResponse;
import com.aditya.worldcup.squads.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;

    @PostMapping
    public SquadResponse createSquad(
            @RequestBody CreateSquadRequest request,
            Authentication authentication
    ) {
        return squadService.createSquad(
                request,
                authentication
        );
    }

    @GetMapping("/my")
    public List<SquadResponse> getMySquads(
            Authentication authentication
    ) {
        return squadService.getMySquads(authentication);
    }
}