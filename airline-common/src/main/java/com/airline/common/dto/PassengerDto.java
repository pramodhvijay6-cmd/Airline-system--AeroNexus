package com.airline.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PassengerDto {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String email;
    private String passportNumber;

    @NotBlank
    private String seatNumber;
}
