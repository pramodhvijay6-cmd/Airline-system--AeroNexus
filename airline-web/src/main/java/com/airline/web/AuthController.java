package com.airline.web;

import com.airline.common.dto.*;
import com.airline.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", null, servletRequest.getRequestURI()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response, servletRequest.getRequestURI()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestParam("token") String token, HttpServletRequest servletRequest) {
        AuthResponse response = authService.refresh(token);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response, servletRequest.getRequestURI()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader, HttpServletRequest servletRequest) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null, servletRequest.getRequestURI()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent", null, servletRequest.getRequestURI()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest servletRequest) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null, servletRequest.getRequestURI()));
    }
}
