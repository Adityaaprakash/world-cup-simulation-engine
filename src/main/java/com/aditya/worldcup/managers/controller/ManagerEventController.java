package com.aditya.worldcup.managers.controller;

import com.aditya.worldcup.managers.dto.EventDecisionRequest;
import com.aditya.worldcup.managers.dto.ManagerEventResponse;
import com.aditya.worldcup.managers.service.ManagerEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/career/events")
@RequiredArgsConstructor
public class ManagerEventController {

    private final ManagerEventService eventService;

    @GetMapping
    public ResponseEntity<List<ManagerEventResponse>> getManagerEvents() {
        return ResponseEntity.ok(eventService.getManagerEvents());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ManagerEventResponse>> getPendingEvents() {
        return ResponseEntity.ok(eventService.getPendingEvents());
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ManagerEventResponse> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @PostMapping("/{eventId}/decide")
    public ResponseEntity<ManagerEventResponse> makeDecision(
            @PathVariable Long eventId, 
            @Valid @RequestBody EventDecisionRequest request) {
        return ResponseEntity.ok(eventService.makeDecision(eventId, request.decisionCode()));
    }
}
