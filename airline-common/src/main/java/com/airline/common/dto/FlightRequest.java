package com.airline.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightRequest {
    @NotBlank
    private String flightNumber;

    @NotNull
    private Long routeId;

    @NotNull
    private Long aircraftId;

    @NotNull
    @Future
    private LocalDateTime departureTime;

    @NotNull
    @Future
    private LocalDateTime arrivalTime;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal basePrice;

    private String status;
}
