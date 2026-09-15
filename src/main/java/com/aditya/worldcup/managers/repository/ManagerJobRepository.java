package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.JobStatus;
import com.aditya.worldcup.managers.entity.ManagerJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManagerJobRepository extends JpaRepository<ManagerJob, Long> {

    Optional<ManagerJob> findByManagerIdAndStatus(Long managerId, JobStatus status);

    List<ManagerJob> findByManagerIdOrderByStartedAtDesc(Long managerId);

    @Query("SELECT j FROM ManagerJob j WHERE j.team.id = :teamId AND j.status = 'ACTIVE'")
    Optional<ManagerJob> findActiveJobForTeam(@Param("teamId") Long teamId);
    
    boolean existsByManagerIdAndStatus(Long managerId, JobStatus status);
}
