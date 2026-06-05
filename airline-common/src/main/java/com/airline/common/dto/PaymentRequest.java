package com.airline.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull
    private Long bookingId;

    @NotBlank
    private String paymentMethod; // CREDIT_CARD or UPI

    @NotBlank
    private String details; // Simulated credit card number or UPI ID
}
