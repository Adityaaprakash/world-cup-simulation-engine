package com.aditya.worldcup.auth.controller;

import com.aditya.worldcup.auth.dto.AuthResponse;
import com.aditya.worldcup.auth.dto.RegisterRequest;
import com.aditya.worldcup.auth.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest request) {

        return authenticationService.register(request);
    }
}