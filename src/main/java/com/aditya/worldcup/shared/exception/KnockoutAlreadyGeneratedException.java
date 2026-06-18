package com.aditya.worldcup.shared.exception;

public class KnockoutAlreadyGeneratedException extends RuntimeException {

    public KnockoutAlreadyGeneratedException() {
        super("Knockout bracket has already been generated for this tournament");
    }
}
