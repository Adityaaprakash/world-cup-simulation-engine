package com.aditya.worldcup.shared.exception;

public class NoRegisteredTeamsException extends RuntimeException {

    public NoRegisteredTeamsException() {
        super("No teams are registered for this tournament");
    }
}
