package com.aditya.worldcup.shared.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Application health checks")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Health check", description = "Returns a lightweight application health payload.")
    @ApiResponse(responseCode = "200", description = "Application is reachable")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "World Cup Simulation Engine",
                "timestamp", LocalDateTime.now()
        );
    }
}
