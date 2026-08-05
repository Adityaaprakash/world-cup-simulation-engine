package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.entity.AdminAuditLog;
import com.aditya.worldcup.admin.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    @Transactional
    public void log(
            String adminUsername,
            String action,
            String entityType,
            Long entityId) {

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUsername(adminUsername)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .build();

        adminAuditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> findActions(
            String username,
            String action,
            String entityType,
            Long entityId,
            LocalDateTime from,
            LocalDateTime to) {

        return adminAuditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .filter(log -> username == null || username.isBlank()
                        || username.equalsIgnoreCase(log.getAdminUsername()))
                .filter(log -> action == null || action.isBlank()
                        || action.equalsIgnoreCase(log.getAction()))
                .filter(log -> entityType == null || entityType.isBlank()
                        || entityType.equalsIgnoreCase(log.getEntityType()))
                .filter(log -> entityId == null || entityId.equals(log.getEntityId()))
                .filter(log -> from == null || !log.getTimestamp().isBefore(from))
                .filter(log -> to == null || !log.getTimestamp().isAfter(to))
                .toList();
    }
}
