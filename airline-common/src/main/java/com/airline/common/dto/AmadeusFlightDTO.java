package com.airline.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO representing a real-time flight offer from the Amadeus API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmadeusFlightDTO {
    private String airline;
    private String airlineCode;
    private String flightNumber;
    private String origin;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private String duration;
    private BigDecimal priceINR;
    private String currency;
    private int numberOfStops;
    private String aircraftType;
    private String cabinClass;
    private int seatsAvailable;
    private String source; // "AMADEUS" to distinguish from local flights
    private boolean bookable; // Whether this can be booked (external = false)
}
