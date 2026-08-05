package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.MaintenanceHistoryResponse;
import com.aditya.worldcup.admin.entity.MaintenanceHistory;
import com.aditya.worldcup.admin.repository.MaintenanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceHistoryService {

    private final MaintenanceHistoryRepository maintenanceHistoryRepository;

    @Transactional
    public void record(
            String operation,
            String administrator,
            long durationMs,
            String status,
            String details) {

        maintenanceHistoryRepository.save(MaintenanceHistory.builder()
                .operation(operation)
                .administrator(administrator)
                .durationMs(durationMs)
                .status(status)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceHistoryResponse> recent() {
        return maintenanceHistoryRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(history -> new MaintenanceHistoryResponse(
                        history.getId(), history.getOperation(),
                        history.getAdministrator(), history.getDurationMs(),
                        history.getStatus(), history.getDetails(),
                        history.getCreatedAt()))
                .toList();
    }
}
