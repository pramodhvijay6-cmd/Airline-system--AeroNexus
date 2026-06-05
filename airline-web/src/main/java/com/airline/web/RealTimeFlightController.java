package com.airline.web;

import com.airline.common.dto.AmadeusFlightDTO;
import com.airline.common.dto.ApiResponse;
import com.airline.service.AmadeusFlightService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for real-time flight pricing via Amadeus API.
 */
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class RealTimeFlightController {

    private final AmadeusFlightService amadeusFlightService;

    /**
     * Search for real-time flight prices from the Amadeus API.
     *
     * @param origin      IATA airport code (e.g., "MAA", "JFK")
     * @param destination IATA airport code (e.g., "BOM", "LAX")
     * @param date        departure date in ISO format (yyyy-MM-dd)
     * @param adults      number of adult passengers (default: 1)
     * @return list of real-time flight offers
     */
    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<List<AmadeusFlightDTO>>> searchRealTimeFlights(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "adults", defaultValue = "1") int adults,
            HttpServletRequest servletRequest) {

        List<AmadeusFlightDTO> flights = amadeusFlightService.searchRealTimeFlights(
                origin.trim().toUpperCase(),
                destination.trim().toUpperCase(),
                date,
                Math.max(1, Math.min(9, adults))
        );

        String message = flights.isEmpty()
                ? "No real-time flight offers found. The Amadeus API may not be configured or no flights are available for this route."
                : "Found " + flights.size() + " real-time flight offers";

        return ResponseEntity.ok(ApiResponse.success(message, flights, servletRequest.getRequestURI()));
    }

    /**
     * Check if the Amadeus real-time pricing API is available.
     */
    @GetMapping("/realtime/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtimeStatus(HttpServletRequest servletRequest) {
        boolean available = amadeusFlightService.isAvailable();
        Map<String, Object> status = Map.of(
                "available", available,
                "provider", "Amadeus Flight Offers Search API",
                "message", available
                        ? "Real-time flight pricing is active"
                        : "Amadeus API credentials not configured. Set AMADEUS_API_KEY and AMADEUS_API_SECRET environment variables."
        );
        return ResponseEntity.ok(ApiResponse.success("Real-time API status", status, servletRequest.getRequestURI()));
    }
}
