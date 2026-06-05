package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.common.dto.PaymentRequest;
import com.airline.common.dto.PaymentResponse;
import com.airline.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request, HttpServletRequest servletRequest) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response, servletRequest.getRequestURI()));
    }
}
