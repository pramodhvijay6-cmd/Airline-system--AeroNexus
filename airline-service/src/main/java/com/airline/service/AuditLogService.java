package com.airline.service;

import com.airline.domain.entity.AuditLog;
import com.airline.domain.entity.User;
import com.airline.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Asynchronously logs an action to the database.
     *
     * @param user the User who triggered the action
     * @param action a description of the action
     * @param entityName the database table/entity target
     * @param entityId the ID of the entity target
     * @param changeLog details of what changed
     */
    @Async
    public void log(User user, String action, String entityName, Long entityId, String changeLog) {
        log.info("Audit log action: '{}' on entity '{}' (ID: {}) by user: {}", 
                action, entityName, entityId, user != null ? user.getUsername() : "SYSTEM");

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .changeLog(changeLog)
                .build();

        auditLogRepository.save(auditLog);
    }
}
