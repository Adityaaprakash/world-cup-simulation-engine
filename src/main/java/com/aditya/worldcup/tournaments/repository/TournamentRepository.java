package com.aditya.worldcup.tournaments.repository;

import com.aditya.worldcup.tournaments.entity.Tournament;
import com.aditya.worldcup.tournaments.entity.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TournamentRepository
        extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {

    boolean existsByNameIgnoreCaseAndYear(String name, Integer year);

    long countByStatus(TournamentStatus status);
}
