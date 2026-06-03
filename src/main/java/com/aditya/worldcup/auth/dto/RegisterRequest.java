package com.aditya.worldcup.auth.dto;

public record RegisterRequest(
        String username,
        String email,
        String password
) {
}