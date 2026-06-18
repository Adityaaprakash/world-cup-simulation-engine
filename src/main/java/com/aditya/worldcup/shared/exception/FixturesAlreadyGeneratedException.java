package com.aditya.worldcup.shared.exception;

public class FixturesAlreadyGeneratedException extends RuntimeException {

    public FixturesAlreadyGeneratedException() {
        super("Fixtures have already been generated for this tournament");
    }
}
