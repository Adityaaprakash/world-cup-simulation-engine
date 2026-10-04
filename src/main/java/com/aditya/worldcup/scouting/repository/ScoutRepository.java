package com.aditya.worldcup.scouting.repository;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.scouting.entity.Scout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoutRepository extends JpaRepository<Scout, Long> {
    List<Scout> findByManagerOrderByNameAsc(Manager manager);
    Optional<Scout> findByIdAndManager(Long id, Manager manager);
    long countByManager(Manager manager);
}
