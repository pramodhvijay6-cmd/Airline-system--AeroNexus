package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.domain.entity.AuditLog;
import com.airline.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(HttpServletRequest servletRequest) {
        Map<String, Object> stats = adminService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success("User statistics retrieved successfully", stats, servletRequest.getRequestURI()));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(HttpServletRequest servletRequest) {
        List<AuditLog> auditLogs = adminService.getAuditLogs();
        return ResponseEntity.ok(ApiResponse.success("System audit logs retrieved successfully", auditLogs, servletRequest.getRequestURI()));
    }
}
