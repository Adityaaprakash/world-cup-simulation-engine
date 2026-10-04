package com.aditya.worldcup.scouting.repository;

import com.aditya.worldcup.managers.entity.Manager;
import com.aditya.worldcup.players.entity.Player;
import com.aditya.worldcup.scouting.entity.ScoutingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoutingReportRepository extends JpaRepository<ScoutingReport, Long> {
    List<ScoutingReport> findByManagerOrderByCreatedAtDesc(Manager manager);
    Optional<ScoutingReport> findByIdAndManager(Long id, Manager manager);
    Optional<ScoutingReport> findByManagerAndPlayer(Manager manager, Player player);
    List<ScoutingReport> findByManagerAndStatus(Manager manager, com.aditya.worldcup.scouting.entity.ReportStatus status);
}
