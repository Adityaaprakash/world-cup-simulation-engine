package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.ManagerObjective;
import com.aditya.worldcup.managers.entity.ObjectiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagerObjectiveRepository extends JpaRepository<ManagerObjective, Long> {
    List<ManagerObjective> findByManagerIdOrderByCreatedAtDesc(Long managerId);
    List<ManagerObjective> findByManagerIdAndStatus(Long managerId, ObjectiveStatus status);
}
