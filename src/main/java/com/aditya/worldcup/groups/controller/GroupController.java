package com.aditya.worldcup.groups.controller;

import com.aditya.worldcup.groups.dto.GroupResponse;
import com.aditya.worldcup.groups.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/{id}/groups/generate")
    public List<GroupResponse> generateGroups(
            @PathVariable Long id) {

        return groupService.generateGroups(id);
    }

    @GetMapping("/{id}/groups")
    public List<GroupResponse> getGroups(
            @PathVariable Long id) {

        return groupService.getGroups(id);
    }
}