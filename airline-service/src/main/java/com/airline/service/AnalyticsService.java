package com.airline.service;

import com.airline.common.model.BookingStatus;
import com.airline.domain.entity.Booking;
import com.airline.domain.entity.Flight;
import com.airline.domain.repository.BookingRepository;
import com.airline.domain.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    /**
     * Aggregates stats including revenue totals, flight occupancies, and recent booking trends.
     *
     * @return a Map containing stat keys and corresponding metric values
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStats() {
        List<Booking> allBookings = bookingRepository.findAll();
        List<Flight> allFlights = flightRepository.findAll();

        // 1. Calculate Total Revenue from Confirmed Bookings
        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Calculate Average Occupancy
        double totalOccupancyRate = 0.0;
        int activeFlightsCount = 0;

        for (Flight flight : allFlights) {
            int capacity = flight.getAircraft().getCapacity();
            if (capacity <= 0) continue;

            long confirmedCount = bookingRepository.findByFlightIdAndBookingStatus(flight.getId(), BookingStatus.CONFIRMED).size();
            double rate = (double) confirmedCount / capacity;
            totalOccupancyRate += rate;
            activeFlightsCount++;
        }

        double averageOccupancy = activeFlightsCount > 0 ? (totalOccupancyRate / activeFlightsCount) * 100 : 0.0;

        // 3. Daily Booking trends (Last 7 days)
        Map<String, Long> trends = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = allBookings.stream()
                    .filter(b -> b.getBookingTime().toLocalDate().equals(date))
                    .count();
            trends.put(date.toString(), count);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageOccupancyPercentage", averageOccupancy);
        stats.put("bookingTrendsLast7Days", trends);
        stats.put("totalBookingsCount", allBookings.size());
        stats.put("activeFlightsCount", allFlights.size());

        return stats;
    }
}
