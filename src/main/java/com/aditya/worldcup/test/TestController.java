package com.aditya.worldcup.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Diagnostics", description = "Authenticated diagnostic endpoint")
public class TestController {

    @GetMapping("/api/test")
    @Operation(summary = "Authentication smoke check", description = "Returns a confirmation message for authenticated requests.")
    @ApiResponse(responseCode = "200", description = "Authenticated request succeeded")
    public String test() {
        return "Authenticated Successfully";
    }
}
