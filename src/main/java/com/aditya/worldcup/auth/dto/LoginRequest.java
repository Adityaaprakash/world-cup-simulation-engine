package com.aditya.worldcup.auth.dto;

public record LoginRequest(
        String email,
        String password
) {
}