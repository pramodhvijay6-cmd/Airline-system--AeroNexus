package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats(HttpServletRequest servletRequest) {
        Map<String, Object> stats = analyticsService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success("System statistics retrieved successfully", stats, servletRequest.getRequestURI()));
    }
}
