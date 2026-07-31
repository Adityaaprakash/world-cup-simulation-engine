package com.aditya.worldcup.tournaments.repository;

import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long> {

    boolean existsByNameIgnoreCaseAndYear(String name, Integer year);

    long countByStatus(TournamentStatus status);
}
