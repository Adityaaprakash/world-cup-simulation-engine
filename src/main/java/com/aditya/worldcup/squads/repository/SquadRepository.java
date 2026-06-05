package com.aditya.worldcup.squads.repository;

import com.aditya.worldcup.squads.entity.Squad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findByUserId(Long userId);

}