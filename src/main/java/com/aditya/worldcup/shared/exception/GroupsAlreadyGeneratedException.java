package com.aditya.worldcup.shared.exception;

public class GroupsAlreadyGeneratedException extends RuntimeException {

    public GroupsAlreadyGeneratedException() {
        super("Groups have already been generated for this tournament");
    }
}
