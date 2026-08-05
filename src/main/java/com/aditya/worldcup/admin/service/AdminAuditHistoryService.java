package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.dto.AuditHistoryResponse;
import com.aditya.worldcup.admin.dto.MaintenanceHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditHistoryService {

    private final AdminAuditService adminAuditService;
    private final MaintenanceHistoryService maintenanceHistoryService;

    @Transactional(readOnly = true)
    public AuditHistoryResponse history(
            String username,
            String action,
            String entityType,
            Long entityId,
            LocalDateTime from,
            LocalDateTime to) {

        List<AuditHistoryResponse.AuditEntry> actions = adminAuditService
                .findActions(username, action, entityType, entityId, from, to)
                .stream()
                .map(log -> new AuditHistoryResponse.AuditEntry(
                        log.getId(), log.getAdminUsername(), log.getAction(),
                        log.getEntityType(), log.getEntityId(), log.getTimestamp()))
                .toList();
        List<MaintenanceHistoryResponse> maintenance = maintenanceHistoryService.recent();
        return new AuditHistoryResponse(actions, maintenance);
    }
}
