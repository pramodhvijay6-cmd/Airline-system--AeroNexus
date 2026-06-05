package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.common.dto.FlightResponse;
import com.airline.service.PredictionService;
import com.airline.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final PredictionService predictionService;
    private final RecommendationService recommendationService;

    @GetMapping("/predict-delay/{flightId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> predictDelay(
            @PathVariable Long flightId, HttpServletRequest servletRequest) {
        Map<String, Object> prediction = predictionService.predictDelay(flightId);
        return ResponseEntity.ok(ApiResponse.success("Flight delay prediction calculated", prediction, servletRequest.getRequestURI()));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<FlightResponse>>> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest servletRequest) {
        List<FlightResponse> recommendations = recommendationService.getRecommendations(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Flight recommendations generated", recommendations, servletRequest.getRequestURI()));
    }
}
