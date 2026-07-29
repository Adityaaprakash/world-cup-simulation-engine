package com.aditya.worldcup.saves.repository;

import com.aditya.worldcup.saves.entity.SaveSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaveSlotRepository extends JpaRepository<SaveSlot, Long> {

    List<SaveSlot> findByManagerIdOrderBySlotNumber(Long managerId);

    Optional<SaveSlot> findByIdAndManagerId(Long id, Long managerId);

    Optional<SaveSlot> findByManagerIdAndAutosaveTrue(
            Long managerId
    );

    Optional<SaveSlot> findByManagerIdAndSlotNumber(
            Long managerId,
            Integer slotNumber
    );

    List<SaveSlot> findByManagerIdAndActiveTrue(Long managerId);

    boolean existsByManagerIdAndSlotNumber(
            Long managerId,
            Integer slotNumber
    );
}
