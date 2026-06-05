package com.airline.service;

import com.airline.common.exception.ResourceNotFoundException;
import com.airline.domain.entity.Flight;
import com.airline.domain.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final FlightRepository flightRepository;

    /**
     * Rules-based delay prediction engine for a given flight.
     * Can easily be upgraded to load an ONNX model.
     *
     * @param flightId the database ID of the flight
     * @return a Map containing predicted delay details
     */
    public Map<String, Object> predictDelay(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + flightId));

        int predictedDelayMinutes = 0;
        double delayProbability = 0.10; // Baseline 10%

        // Rule 1: Time of departure peak hours check (8am-10am, 5pm-8pm)
        LocalTime depTime = flight.getDepartureTime().toLocalTime();
        if ((depTime.isAfter(LocalTime.of(7, 59)) && depTime.isBefore(LocalTime.of(10, 1))) ||
            (depTime.isAfter(LocalTime.of(16, 59)) && depTime.isBefore(LocalTime.of(20, 1)))) {
            predictedDelayMinutes += 20;
            delayProbability += 0.25;
        }

        // Rule 2: Route distance factor
        int distance = flight.getRoute().getDistanceMiles();
        if (distance > 1500) {
            predictedDelayMinutes += 15;
            delayProbability += 0.15;
        } else if (distance > 800) {
            predictedDelayMinutes += 5;
            delayProbability += 0.05;
        }

        // Rule 3: Dynamic scheduling density / season check
        int monthValue = flight.getDepartureTime().getMonthValue();
        if (monthValue == 12 || monthValue == 6 || monthValue == 7) { // Holiday seasons
            predictedDelayMinutes += 12;
            delayProbability += 0.20;
        }

        // Bound probability to max 0.95
        delayProbability = Math.min(0.95, delayProbability);

        Map<String, Object> result = new HashMap<>();
        result.put("flightId", flightId);
        result.put("flightNumber", flight.getFlightNumber());
        result.put("predictedDelayMinutes", predictedDelayMinutes);
        result.put("delayProbability", Math.round(delayProbability * 100.0) / 100.0);
        result.put("weatherConditionMock", "CLEAR_SKY");
        result.put("recommendationMessage", predictedDelayMinutes > 15 ? 
                "Higher chance of delay. Monitor gate information." : "Low risk of delay. Flight is on-schedule.");

        log.info("Predicted delay for flight {}: {} min with {} probability", 
                flight.getFlightNumber(), predictedDelayMinutes, delayProbability);
        return result;
    }
}
