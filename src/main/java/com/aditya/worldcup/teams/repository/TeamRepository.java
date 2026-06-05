package com.aditya.worldcup.teams.repository;

import com.aditya.worldcup.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}