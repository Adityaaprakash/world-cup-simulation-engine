package com.aditya.worldcup.formations.repository;

import com.aditya.worldcup.formations.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationRepository
        extends JpaRepository<Formation, Long> {
}