package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.domain.entity.Passenger;
import com.airline.service.PassengerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PutMapping("/{id}/passport")
    public ResponseEntity<ApiResponse<Passenger>> updatePassport(
            @PathVariable Long id, @RequestParam("passportNumber") String passportNumber, HttpServletRequest servletRequest) {
        Passenger passenger = passengerService.updatePassportInfo(id, passportNumber);
        return ResponseEntity.ok(ApiResponse.success("Passenger passport updated successfully", passenger, servletRequest.getRequestURI()));
    }
}
