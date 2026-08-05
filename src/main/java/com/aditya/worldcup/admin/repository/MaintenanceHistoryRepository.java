package com.aditya.worldcup.admin.repository;

import com.aditya.worldcup.admin.entity.MaintenanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceHistoryRepository
        extends JpaRepository<MaintenanceHistory, Long> {

    List<MaintenanceHistory> findTop50ByOrderByCreatedAtDesc();
}
