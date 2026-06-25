package com.aditya.worldcup.shared.exception;

public class GroupStageAlreadyCompletedException extends RuntimeException {

    public GroupStageAlreadyCompletedException() {
        super("Group stage has already been completed for this tournament");
    }
}
