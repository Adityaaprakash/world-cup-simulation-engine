package com.aditya.worldcup.shared.exception;

public class RegistrationClosedException extends RuntimeException {

    public RegistrationClosedException() {
        super("Registration is closed for this tournament");
    }
}
