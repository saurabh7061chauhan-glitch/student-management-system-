package com.example.studentmanagement.service;

import com.example.studentmanagement.entity.AuditLog;
import com.example.studentmanagement.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String username, String role, String action, String entityType, String entityId, String details, String ipAddress) {
        try {
            AuditLog log = new AuditLog(
                    username != null ? username : "System",
                    role != null ? role : "ANONYMOUS",
                    action,
                    entityType,
                    entityId,
                    details,
                    ipAddress != null ? ipAddress : "127.0.0.1"
            );
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Silently swallow so logging failure never blocks critical transactions
        }
    }

    public void log(String username, String role, String action, String entityType, String entityId, String details) {
        log(username, role, action, entityType, entityId, details, "127.0.0.1");
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }

    public Page<AuditLog> getLogs(String query, String role, String action, LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
        String cleanAction = (action != null && !action.trim().isEmpty()) ? action.trim() : null;
        return auditLogRepository.searchAuditLogs(cleanQuery, cleanRole, cleanAction, start, end, pageable);
    }

    public Page<AuditLog> searchLogs(String search, String role, String action, Pageable pageable) {
        String cleanQuery = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
        String cleanAction = (action != null && !action.trim().isEmpty()) ? action.trim() : null;
        return auditLogRepository.searchAuditLogs(cleanQuery, cleanRole, cleanAction, null, null, pageable);
    }

    public void logAction(String role, String action, String entityType, Long entityId, String details) {
        log("ADMIN", role, action, entityType, entityId != null ? String.valueOf(entityId) : null, details);
    }

    public void logAction(String username, String role, String action, String entityType, Long entityId, String details) {
        log(username, role, action, entityType, entityId != null ? String.valueOf(entityId) : null, details);
    }
}
