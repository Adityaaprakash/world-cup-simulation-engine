package com.aditya.worldcup.shared.exception;

public class TeamAlreadyRegisteredException extends RuntimeException {

    public TeamAlreadyRegisteredException() {
        super("Team already registered for this tournament");
    }
}
