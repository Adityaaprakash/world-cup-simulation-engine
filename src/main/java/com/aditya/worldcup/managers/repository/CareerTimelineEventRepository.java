package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.CareerTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerTimelineEventRepository
        extends JpaRepository<CareerTimelineEvent, Long> {

    List<CareerTimelineEvent> findByManagerIdOrderByOccurredAtDesc(Long managerId);
}
