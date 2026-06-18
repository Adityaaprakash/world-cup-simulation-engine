package com.aditya.worldcup.shared.exception;

public class FixturesNotGeneratedException extends RuntimeException {

    public FixturesNotGeneratedException() {
        super("Fixtures have not been generated for this tournament");
    }
}
