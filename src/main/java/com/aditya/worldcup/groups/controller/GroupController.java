package com.aditya.worldcup.groups.controller;

import com.aditya.worldcup.groups.dto.GroupResponse;
import com.aditya.worldcup.groups.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Groups", description = "Tournament group generation and lookup")
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/{id}/groups/generate")
    @Operation(summary = "Generate groups", description = "Generates tournament groups for registered teams.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groups generated"),
            @ApiResponse(responseCode = "404", description = "Tournament not found"),
            @ApiResponse(responseCode = "409", description = "Groups already generated or tournament state invalid")
    })
    public List<GroupResponse> generateGroups(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return groupService.generateGroups(id);
    }

    @GetMapping("/{id}/groups")
    @Operation(summary = "List groups", description = "Returns generated groups for a tournament.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groups returned"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public List<GroupResponse> getGroups(
            @Parameter(description = "Tournament id")
            @PathVariable @Positive Long id) {

        return groupService.getGroups(id);
    }
}
