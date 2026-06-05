package com.airline.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private String status;
    private BigDecimal totalPrice;
    private FlightResponse flight;
    private List<PassengerResponseDto> passengers;
    private LocalDateTime bookingTime;
}
