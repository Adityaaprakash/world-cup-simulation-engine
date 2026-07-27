package com.aditya.worldcup.managers.controller;

import com.aditya.worldcup.managers.dto.CareerHistoryResponse;
import com.aditya.worldcup.managers.dto.CareerStatisticsResponse;
import com.aditya.worldcup.managers.dto.ManagerResponse;
import com.aditya.worldcup.managers.service.CareerHistoryService;
import com.aditya.worldcup.managers.service.CareerStatisticsService;
import com.aditya.worldcup.managers.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
@Tag(name = "Managers", description = "Authenticated manager career profile")
public class ManagerController {

    private final ManagerService managerService;
    private final CareerStatisticsService careerStatisticsService;
    private final CareerHistoryService careerHistoryService;

    @GetMapping("/me")
    @Operation(summary = "Get my manager career", description = "Returns the authenticated user's manager career profile, creating a default profile if needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Manager career returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ManagerResponse getCurrentManager(Authentication authentication) {
        return managerService.getCurrentManager(authentication);
    }

    @GetMapping("/me/statistics")
    @Operation(summary = "Get my career statistics", description = "Returns aggregate manager career statistics for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Career statistics returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public CareerStatisticsResponse getCurrentStatistics(
            Authentication authentication) {

        return careerStatisticsService.getCurrentStatistics(authentication);
    }

    @GetMapping("/me/history")
    @Operation(summary = "Get my career history", description = "Returns completed tournament history for the authenticated user's manager career.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Career history returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<CareerHistoryResponse> getCurrentHistory(
            Authentication authentication) {

        return careerHistoryService.getCurrentHistory(authentication);
    }
}
