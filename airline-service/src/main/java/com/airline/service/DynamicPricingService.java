package com.airline.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class DynamicPricingService {

    /**
     * Calculates the dynamic price of a flight based on base price, departure date, and occupancy.
     *
     * @param basePrice the base price of the flight
     * @param departureTime the flight departure time
     * @param totalSeats the total capacity of the aircraft
     * @param bookedSeats the number of booked seats
     * @return the calculated dynamic price
     */
    public BigDecimal calculatePrice(BigDecimal basePrice, LocalDateTime departureTime, int totalSeats, int bookedSeats) {
        if (basePrice == null) return BigDecimal.ZERO;

        // 1. Days to departure multiplier
        double dateMultiplier = 1.0;
        long daysToDeparture = ChronoUnit.DAYS.between(LocalDateTime.now(), departureTime);

        if (daysToDeparture < 7) {
            dateMultiplier = 1.5;
        } else if (daysToDeparture < 14) {
            dateMultiplier = 1.25;
        } else if (daysToDeparture < 30) {
            dateMultiplier = 1.1;
        }

        // 2. Occupancy multiplier
        double occupancyMultiplier = 1.0;
        double occupancyRate = totalSeats > 0 ? (double) bookedSeats / totalSeats : 0.0;

        if (occupancyRate > 0.8) {
            occupancyMultiplier = 1.6;
        } else if (occupancyRate > 0.6) {
            occupancyMultiplier = 1.3;
        } else if (occupancyRate > 0.3) {
            occupancyMultiplier = 1.15;
        }

        // Calculate current price
        BigDecimal dynamicFactor = BigDecimal.valueOf(dateMultiplier * occupancyMultiplier);
        return basePrice.multiply(dynamicFactor).setScale(2, RoundingMode.HALF_UP);
    }
}
