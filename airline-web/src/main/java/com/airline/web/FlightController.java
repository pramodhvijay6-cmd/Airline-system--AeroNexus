package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.common.dto.FlightRequest;
import com.airline.common.dto.FlightResponse;
import com.airline.service.FlightService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<FlightResponse>> createFlight(
            @Valid @RequestBody FlightRequest request, HttpServletRequest servletRequest) {
        FlightResponse response = flightService.createFlight(request);
        return ResponseEntity.ok(ApiResponse.success("Flight created successfully", response, servletRequest.getRequestURI()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<FlightResponse>> updateFlight(
            @PathVariable Long id, @Valid @RequestBody FlightRequest request, HttpServletRequest servletRequest) {
        FlightResponse response = flightService.updateFlight(id, request);
        return ResponseEntity.ok(ApiResponse.success("Flight updated successfully", response, servletRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightResponse>> getFlightById(
            @PathVariable Long id, HttpServletRequest servletRequest) {
        FlightResponse response = flightService.getFlightById(id);
        return ResponseEntity.ok(ApiResponse.success("Flight details retrieved successfully", response, servletRequest.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlightResponse>>> getAllFlights(HttpServletRequest servletRequest) {
        List<FlightResponse> response = flightService.getAllFlights();
        return ResponseEntity.ok(ApiResponse.success("All flights retrieved successfully", response, servletRequest.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFlight(
            @PathVariable Long id, HttpServletRequest servletRequest) {
        flightService.deleteFlight(id);
        return ResponseEntity.ok(ApiResponse.success("Flight deleted successfully", null, servletRequest.getRequestURI()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FlightResponse>>> searchFlights(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("departureDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            HttpServletRequest servletRequest) {
        List<FlightResponse> response = flightService.searchFlights(origin, destination, departureDate);
        return ResponseEntity.ok(ApiResponse.success("Flights searched successfully", response, servletRequest.getRequestURI()));
    }
}
