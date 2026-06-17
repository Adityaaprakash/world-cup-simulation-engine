package com.aditya.worldcup.matches.repository;

import com.aditya.worldcup.matches.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository
        extends JpaRepository<Match, Long> {
}