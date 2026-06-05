package com.airline.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String passportNumber;
    private String seatNumber;
    private String ticketNumber;
}
