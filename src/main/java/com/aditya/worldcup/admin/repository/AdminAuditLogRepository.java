package com.aditya.worldcup.admin.repository;

import com.aditya.worldcup.admin.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLog, Long> {

    java.util.List<AdminAuditLog> findAllByOrderByTimestampDesc();
}
