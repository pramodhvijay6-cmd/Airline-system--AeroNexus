package com.airline.service;

import com.airline.common.dto.FlightResponse;
import com.airline.domain.entity.Booking;
import com.airline.domain.entity.Flight;
import com.airline.domain.entity.User;
import com.airline.domain.repository.BookingRepository;
import com.airline.domain.repository.FlightRepository;
import com.airline.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final FlightService flightService;

    /**
     * Recommends top-5 flights based on user route history.
     *
     * @param username the username of the user to recommend flights for
     * @return a list of recommended FlightResponse details
     */
    @Transactional(readOnly = true)
    public List<FlightResponse> getRecommendations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<Booking> userBookings = bookingRepository.findByUser(user);

        // Retrieve most frequent destinations
        List<String> preferredDestinations = userBookings.stream()
                .map(b -> b.getFlight().getRoute().getDestination())
                .collect(Collectors.groupingBy(dest -> dest, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Flight> allUpcomingFlights = flightRepository.findAll().stream()
                .filter(f -> f.getDepartureTime().isAfter(LocalDateTime.now()) && f.getStatus() == com.airline.common.model.FlightStatus.SCHEDULED)
                .collect(Collectors.toList());

        if (preferredDestinations.isEmpty()) {
            // Fallback: Return top 5 upcoming flights sorted by departure time
            return allUpcomingFlights.stream()
                    .sorted(Comparator.comparing(Flight::getDepartureTime))
                    .limit(5)
                    .map(flightService::mapToResponse)
                    .collect(Collectors.toList());
        }

        // Recommend flights going to preferred destinations
        List<Flight> recommended = allUpcomingFlights.stream()
                .filter(f -> preferredDestinations.contains(f.getRoute().getDestination()))
                .sorted(Comparator.comparing(Flight::getDepartureTime))
                .limit(5)
                .collect(Collectors.toList());

        // Fill up to 5 if needed
        if (recommended.size() < 5) {
            int needed = 5 - recommended.size();
            List<Flight> fillUps = allUpcomingFlights.stream()
                    .filter(f -> !recommended.contains(f))
                    .sorted(Comparator.comparing(Flight::getDepartureTime))
                    .limit(needed)
                    .toList();
            recommended.addAll(fillUps);
        }

        log.info("Returned {} flight recommendations for user: {}", recommended.size(), username);
        return recommended.stream()
                .map(flightService::mapToResponse)
                .collect(Collectors.toList());
    }
}
