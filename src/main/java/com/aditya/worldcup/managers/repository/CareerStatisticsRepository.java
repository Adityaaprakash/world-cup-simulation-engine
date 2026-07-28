package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.CareerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareerStatisticsRepository
        extends JpaRepository<CareerStatistics, Long> {

    Optional<CareerStatistics> findByManagerId(Long managerId);

    List<CareerStatistics> findAllByOrderByWinsDesc();
}
