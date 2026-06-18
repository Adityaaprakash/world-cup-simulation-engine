package com.aditya.worldcup.shared.exception;

public class GroupsNotGeneratedException extends RuntimeException {

    public GroupsNotGeneratedException() {
        super("Groups have not been generated for this tournament");
    }
}
