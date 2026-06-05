package com.airline.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull
    private Long flightId;

    private String couponCode;

    @NotEmpty
    @Valid
    private List<PassengerDto> passengers;
}
