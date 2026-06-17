package com.aditya.worldcup.tournaments.repository;

import com.aditya.worldcup.tournaments.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long> {
}