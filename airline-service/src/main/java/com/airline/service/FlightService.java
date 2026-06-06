package com.airline.service;

import com.airline.common.dto.FlightRequest;
import com.airline.common.dto.FlightResponse;
import com.airline.common.exception.ResourceNotFoundException;
import com.airline.common.model.BookingStatus;
import com.airline.common.model.FlightStatus;
import com.airline.domain.entity.Aircraft;
import com.airline.domain.entity.Flight;
import com.airline.domain.entity.Route;
import com.airline.domain.repository.AircraftRepository;
import com.airline.domain.repository.BookingRepository;
import com.airline.domain.repository.FlightRepository;
import com.airline.domain.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing flight creation, scheduling, details retrieval, and flight search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightService {

    private final FlightRepository flightRepository;
    private final RouteRepository routeRepository;
    private final AircraftRepository aircraftRepository;
    private final BookingRepository bookingRepository;
    private final DynamicPricingService dynamicPricingService;

    /**
     * Creates and schedules a new flight.
     *
     * @param request the flight details request
     * @return the created FlightResponse details
     */
    @Transactional
    public FlightResponse createFlight(FlightRequest request) {
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + request.getAircraftId()));

        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .route(route)
                .aircraft(aircraft)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .basePrice(request.getBasePrice())
                .status(request.getStatus() != null ? FlightStatus.valueOf(request.getStatus()) : FlightStatus.SCHEDULED)
                .build();

        Flight savedFlight = flightRepository.save(flight);
        log.info("Created flight: {}", savedFlight.getFlightNumber());
        return mapToResponse(savedFlight);
    }

    /**
     * Updates details of an existing flight.
     *
     * @param id the database ID of the flight
     * @param request the new flight details request
     * @return the updated FlightResponse details
     */
    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + id));
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + request.getAircraftId()));

        flight.setFlightNumber(request.getFlightNumber());
        flight.setRoute(route);
        flight.setAircraft(aircraft);
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setBasePrice(request.getBasePrice());
        if (request.getStatus() != null) {
            flight.setStatus(FlightStatus.valueOf(request.getStatus()));
        }

        Flight savedFlight = flightRepository.save(flight);
        log.info("Updated flight: {}", savedFlight.getFlightNumber());
        return mapToResponse(savedFlight);
    }

    /**
     * Retrieves flight information by ID.
     *
     * @param id the database ID of the flight
     * @return FlightResponse details
     */
    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + id));
        return mapToResponse(flight);
    }

    /**
     * Retrieves all flights.
     *
     * @return a list of all flights
     */
    @Transactional(readOnly = true)
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a flight.
     *
     * @param id the database ID of the flight
     */
    @Transactional
    public void deleteFlight(Long id) {
        if (!flightRepository.existsById(id)) {
            throw new ResourceNotFoundException("Flight not found with id: " + id);
        }
        flightRepository.deleteById(id);
        log.info("Deleted flight with id: {}", id);
    }

    /**
     * Searches for flights based on route and date.
     *
     * @param origin the IATA departure airport code
     * @param destination the IATA destination airport code
     * @param departureDate the date of departure
     * @return list of matching FlightResponse details
     */
    @Transactional
    public List<FlightResponse> searchFlights(String origin, String destination, LocalDate departureDate) {
        LocalDateTime startTime = departureDate.atStartOfDay();
        LocalDateTime endTime = departureDate.plusDays(1).atStartOfDay();

        List<String> origins = extractSearchKeys(origin);
        List<String> destinations = extractSearchKeys(destination);

        List<Flight> flights = flightRepository.searchFlights(origins, destinations, startTime, endTime);
        if (flights.isEmpty()) {
            flights = generateMockFlightsOnTheFly(origin, destination, departureDate);
        }

        return flights.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private List<Flight> generateMockFlightsOnTheFly(String origin, String destination, LocalDate departureDate) {
        String normOrigin = com.airline.common.util.AirportResolver.resolveToIata(origin);
        String normDestination = com.airline.common.util.AirportResolver.resolveToIata(destination);

        // 1. Create route if not exists
        Route route = routeRepository.findByOriginAndDestination(normOrigin, normDestination)
                .orElseGet(() -> {
                    int distance = estimateDistanceInMiles(normOrigin, normDestination);
                    int duration = (int)(distance / 8.0) + 40;
                    Route newRoute = Route.builder()
                            .origin(normOrigin)
                            .destination(normDestination)
                            .distanceMiles(distance)
                            .durationMinutes(duration)
                            .build();
                    Route saved = routeRepository.save(newRoute);
                    log.info("Dynamically generated new route: {} ➔ {}", normOrigin, normDestination);
                    return saved;
                });

        // 2. Fetch active aircrafts
        List<Aircraft> aircrafts = aircraftRepository.findAll();
        if (aircrafts.isEmpty()) {
            log.warn("No aircrafts found to schedule mock flight.");
            return java.util.Collections.emptyList();
        }

        List<Flight> generated = new java.util.ArrayList<>();
        String dateStr = departureDate.toString();
        java.util.Random rand = new java.util.Random();

        // Calculate mock price in INR
        int distance = route.getDistanceMiles();
        double rate = 5.0 + rand.nextDouble() * 3.0; // 5.0 to 8.0 INR per mile
        double calcPrice1 = Math.max(2500, distance * rate);
        double calcPrice2 = calcPrice1 * 1.15;

        // Flight 1: Morning
        String fn1 = "AN-" + (100 + rand.nextInt(900)) + "-" + dateStr;
        Aircraft ac1 = aircrafts.get(rand.nextInt(aircrafts.size()));
        LocalDateTime dep1 = departureDate.atTime(8, 30);
        LocalDateTime arr1 = dep1.plusMinutes(route.getDurationMinutes());
        java.math.BigDecimal price1 = java.math.BigDecimal.valueOf(Math.round(calcPrice1 / 100.0) * 100);
        
        // Flight 2: Afternoon
        String fn2 = "AN-" + (100 + rand.nextInt(900)) + "-" + dateStr;
        Aircraft ac2 = aircrafts.get(rand.nextInt(aircrafts.size()));
        LocalDateTime dep2 = departureDate.atTime(15, 15);
        LocalDateTime arr2 = dep2.plusMinutes(route.getDurationMinutes());
        java.math.BigDecimal price2 = java.math.BigDecimal.valueOf(Math.round(calcPrice2 / 100.0) * 100);

        generated.add(createFlightEntityOnTheFly(fn1, route, ac1, dep1, arr1, price1));
        generated.add(createFlightEntityOnTheFly(fn2, route, ac2, dep2, arr2, price2));

        return generated;
    }

    private Flight createFlightEntityOnTheFly(String flightNumber, Route route, Aircraft aircraft,
                                              LocalDateTime departure, LocalDateTime arrival, java.math.BigDecimal basePrice) {
        return flightRepository.findByFlightNumber(flightNumber).orElseGet(() -> {
            Flight f = Flight.builder()
                    .flightNumber(flightNumber)
                    .route(route)
                    .aircraft(aircraft)
                    .departureTime(departure)
                    .arrivalTime(arrival)
                    .basePrice(basePrice)
                    .status(FlightStatus.SCHEDULED)
                    .build();
            Flight saved = flightRepository.save(f);
            log.info("Dynamically generated new flight: {} ({} ➔ {}) on {}", flightNumber, route.getOrigin(), route.getDestination(), departure.toLocalDate());
            return saved;
        });
    }

    private List<String> extractSearchKeys(String input) {
        if (input == null) return java.util.Collections.emptyList();
        String cleaned = input.trim().toUpperCase();
        List<String> keys = new java.util.ArrayList<>();
        keys.add(cleaned);
        
        // Resolve city/state name to IATA code
        String resolvedIata = com.airline.common.util.AirportResolver.resolveToIata(cleaned);
        if (!resolvedIata.equalsIgnoreCase(cleaned)) {
            keys.add(resolvedIata);
        }
        
        if (cleaned.contains("(") && cleaned.contains(")")) {
            int openIdx = cleaned.indexOf('(');
            int closeIdx = cleaned.indexOf(')');
            if (closeIdx > openIdx) {
                String firstPart = cleaned.substring(0, openIdx).trim();
                String parenPart = cleaned.substring(openIdx + 1, closeIdx).trim();
                if (!firstPart.isEmpty()) keys.add(firstPart);
                if (!parenPart.isEmpty()) keys.add(parenPart);
            }
        }
        return keys;
    }

    /**
     * Map a Flight entity to a FlightResponse with dynamic pricing and capacity details.
     *
     * @param flight the Flight entity
     * @return populated FlightResponse
     */
    public FlightResponse mapToResponse(Flight flight) {
        int totalCapacity = flight.getAircraft().getCapacity();

        // Get count of confirmed and pending bookings for this flight
        long bookedSeats = bookingRepository.findByFlightIdAndBookingStatus(flight.getId(), BookingStatus.CONFIRMED).size()
                         + bookingRepository.findByFlightIdAndBookingStatus(flight.getId(), BookingStatus.PENDING).size();

        int availableSeats = Math.max(0, totalCapacity - (int) bookedSeats);

        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getRoute().getOrigin())
                .destination(flight.getRoute().getDestination())
                .aircraftModel(flight.getAircraft().getModel())
                .tailNumber(flight.getAircraft().getTailNumber())
                .capacity(totalCapacity)
                .availableSeats(availableSeats)
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .status(flight.getStatus().name())
                .basePrice(flight.getBasePrice())
                .currentPrice(dynamicPricingService.calculatePrice(
                        flight.getBasePrice(),
                        flight.getDepartureTime(),
                        totalCapacity,
                        (int) bookedSeats
                ))
                .build();
    }

    private int estimateDistanceInMiles(String origin, String destination) {
        String routeKey = origin + "-" + destination;
        
        java.util.Map<String, Integer> exactDistances = new java.util.HashMap<>();
        exactDistances.put("MAA-HYD", 320);
        exactDistances.put("HYD-MAA", 320);
        exactDistances.put("MAA-BLR", 180);
        exactDistances.put("BLR-MAA", 180);
        exactDistances.put("BLR-HYD", 310);
        exactDistances.put("HYD-BLR", 310);
        exactDistances.put("MAA-CCU", 850);
        exactDistances.put("CCU-MAA", 850);
        exactDistances.put("DEL-HYD", 780);
        exactDistances.put("HYD-DEL", 780);
        exactDistances.put("BOM-HYD", 385);
        exactDistances.put("HYD-BOM", 385);
        
        if (exactDistances.containsKey(routeKey)) {
            return exactDistances.get(routeKey);
        }
        
        java.util.Set<String> indianAirports = java.util.Set.of("DEL", "BOM", "MAA", "BLR", "HYD", "CCU", "GOI", "COK", "PNQ", "AMD", "JAI", "LKO");
        java.util.Set<String> usAirports = java.util.Set.of("JFK", "LAX", "ORD", "DFW", "MIA", "SFO", "ATL", "SEA", "BOS", "DEN");
        
        boolean originIndia = indianAirports.contains(origin);
        boolean destIndia = indianAirports.contains(destination);
        boolean originUS = usAirports.contains(origin);
        boolean destUS = usAirports.contains(destination);
        
        if (originIndia && destIndia) {
            return 300 + (int)(Math.random() * 500); // 300 to 800 miles
        } else if (originUS && destUS) {
            return 800 + (int)(Math.random() * 1500); // 800 to 2300 miles
        } else if (originIndia || destIndia) {
            return 3000 + (int)(Math.random() * 2000); // 3000 to 5000 miles
        } else {
            return 1000 + (int)(Math.random() * 3000); // 1000 to 4000 miles
        }
    }
}
