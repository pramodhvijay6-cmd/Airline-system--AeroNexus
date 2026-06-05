package com.airline.service;

import com.airline.domain.entity.AuditLog;
import com.airline.domain.entity.User;
import com.airline.domain.repository.AuditLogRepository;
import com.airline.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Gathers user base counts.
     *
     * @return map of user statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        List<User> users = userRepository.findAll();
        long total = users.size();
        long active = users.stream().filter(User::isEnabled).count();
        long disabled = total - active;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", total);
        stats.put("activeUsers", active);
        stats.put("disabledUsers", disabled);
        return stats;
    }

    /**
     * Lists system changes in descending order.
     *
     * @return list of audit log entities
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
