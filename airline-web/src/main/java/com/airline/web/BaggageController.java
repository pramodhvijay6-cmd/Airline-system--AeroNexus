package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.domain.entity.Baggage;
import com.airline.service.BaggageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/baggage")
@RequiredArgsConstructor
public class BaggageController {

    private final BaggageService baggageService;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<Baggage>> checkIn(
            @RequestParam("passengerId") Long passengerId,
            @RequestParam("bagTag") String bagTag,
            @RequestParam("weight") BigDecimal weight,
            HttpServletRequest servletRequest) {
        Baggage baggage = baggageService.checkInBaggage(passengerId, bagTag, weight);
        return ResponseEntity.ok(ApiResponse.success("Baggage checked in successfully", baggage, servletRequest.getRequestURI()));
    }

    @PutMapping("/{bagTag}/status")
    public ResponseEntity<ApiResponse<Baggage>> updateStatus(
            @PathVariable String bagTag,
            @RequestParam("status") String status,
            HttpServletRequest servletRequest) {
        Baggage baggage = baggageService.updateBaggageStatus(bagTag, status);
        return ResponseEntity.ok(ApiResponse.success("Baggage status updated successfully", baggage, servletRequest.getRequestURI()));
    }

    @GetMapping("/{bagTag}")
    public ResponseEntity<ApiResponse<Baggage>> getByTag(
            @PathVariable String bagTag, HttpServletRequest servletRequest) {
        Baggage baggage = baggageService.getBaggageByTag(bagTag);
        return ResponseEntity.ok(ApiResponse.success("Baggage tracked successfully", baggage, servletRequest.getRequestURI()));
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<ApiResponse<List<Baggage>>> getByPassenger(
            @PathVariable Long passengerId, HttpServletRequest servletRequest) {
        List<Baggage> baggage = baggageService.getBaggageByPassenger(passengerId);
        return ResponseEntity.ok(ApiResponse.success("Baggage list retrieved successfully", baggage, servletRequest.getRequestURI()));
    }
}
