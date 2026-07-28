package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.ManagerCareerAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerCareerAnalyticsRepository
        extends JpaRepository<ManagerCareerAnalytics, Long> {

    Optional<ManagerCareerAnalytics> findByManagerId(Long managerId);
}
