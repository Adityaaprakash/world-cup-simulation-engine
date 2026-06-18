package com.aditya.worldcup.shared.exception;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(Long teamId) {
        super("Team not found with id: " + teamId);
    }
}
