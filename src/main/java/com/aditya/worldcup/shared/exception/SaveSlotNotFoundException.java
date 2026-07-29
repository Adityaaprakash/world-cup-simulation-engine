package com.aditya.worldcup.shared.exception;

public class SaveSlotNotFoundException extends RuntimeException {

    public SaveSlotNotFoundException(Long id) {
        super("Save slot not found with id: " + id);
    }
}
