package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.common.dto.BookingRequest;
import com.airline.common.dto.BookingResponse;
import com.airline.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest servletRequest) {
        BookingResponse response = bookingService.createBooking(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Booking initiated successfully", response, servletRequest.getRequestURI()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable Long id,
            HttpServletRequest servletRequest) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", null, servletRequest.getRequestURI()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest servletRequest) {
        List<BookingResponse> response = bookingService.getUserBookings(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User bookings retrieved successfully", response, servletRequest.getRequestURI()));
    }

    @GetMapping("/flight/{flightId}/occupied-seats")
    public ResponseEntity<ApiResponse<List<String>>> getOccupiedSeats(
            @PathVariable Long flightId,
            HttpServletRequest servletRequest) {
        List<String> response = bookingService.getOccupiedSeats(flightId);
        return ResponseEntity.ok(ApiResponse.success("Occupied seats retrieved successfully", response, servletRequest.getRequestURI()));
    }
}
