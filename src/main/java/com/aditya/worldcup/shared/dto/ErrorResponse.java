package com.aditya.worldcup.shared.dto;

import java.time.LocalDateTime;

public record ErrorResponse(

        LocalDateTime timestamp,

        int status,

        String error,

        String message,

        String path

) {

    public ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message
    ) {
        this(timestamp, status, error, message, null);
    }
}
