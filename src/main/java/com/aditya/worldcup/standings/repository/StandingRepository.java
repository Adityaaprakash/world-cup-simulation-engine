package com.aditya.worldcup.standings.repository;

import com.aditya.worldcup.standings.entity.Standing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandingRepository
        extends JpaRepository<Standing, Long> {
}