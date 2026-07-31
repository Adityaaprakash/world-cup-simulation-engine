package com.aditya.worldcup.admin.service;

import com.aditya.worldcup.admin.entity.AdminAuditLog;
import com.aditya.worldcup.admin.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
}
