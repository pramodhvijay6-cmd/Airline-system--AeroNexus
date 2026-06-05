package com.airline.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private String origin;
    private String destination;
    private String aircraftModel;
    private String tailNumber;
    private int capacity;
    private int availableSeats;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String status;
    private BigDecimal basePrice;
    private BigDecimal currentPrice;
}
