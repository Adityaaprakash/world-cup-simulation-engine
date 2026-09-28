package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.ManagerEvent;
import com.aditya.worldcup.managers.entity.ManagerEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerEventRepository extends JpaRepository<ManagerEvent, Long> {
    List<ManagerEvent> findByManagerIdOrderByCreatedAtDesc(Long managerId);
    List<ManagerEvent> findByManagerIdAndStatusOrderByCreatedAtDesc(Long managerId, ManagerEventStatus status);
    Optional<ManagerEvent> findByManagerIdAndContextId(Long managerId, String contextId);
    Optional<ManagerEvent> findByIdAndManagerId(Long id, Long managerId);
}
