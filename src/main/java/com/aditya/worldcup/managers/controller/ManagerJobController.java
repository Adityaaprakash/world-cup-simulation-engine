package com.aditya.worldcup.managers.controller;

import com.aditya.worldcup.managers.dto.ManagerJobRequest;
import com.aditya.worldcup.managers.dto.ManagerJobResponse;
import com.aditya.worldcup.managers.service.ManagerJobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers/jobs")
@RequiredArgsConstructor
public class ManagerJobController {

    private final ManagerJobService managerJobService;

    @PostMapping("/accept")
    public ResponseEntity<ManagerJobResponse> acceptJob(
            Authentication authentication,
            @Valid @RequestBody ManagerJobRequest request
    ) {
        return ResponseEntity.ok(managerJobService.acceptJob(authentication, request));
    }

    @PostMapping("/resign")
    public ResponseEntity<ManagerJobResponse> resignFromJob(
            Authentication authentication
    ) {
        return ResponseEntity.ok(managerJobService.resignFromJob(authentication));
    }

    @GetMapping
    public ResponseEntity<List<ManagerJobResponse>> getMyJobs(
            Authentication authentication
    ) {
        return ResponseEntity.ok(managerJobService.getMyJobs(authentication));
    }
}
