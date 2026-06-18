package com.aditya.worldcup.shared.exception;

public class TournamentNotFoundException extends RuntimeException {

    public TournamentNotFoundException(Long tournamentId) {
        super("Tournament not found with id: " + tournamentId);
    }
}
