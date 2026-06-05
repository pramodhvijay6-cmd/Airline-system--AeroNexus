package com.airline.service;

import com.airline.common.dto.AmadeusFlightDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Generates realistic real-time flight prices based on real airline data,
 * route distances, seasonal demand, and market conditions.
 * Works without any external API keys.
 */
@Service
@Slf4j
public class RealTimePriceEngine {

    // ──── Real airline data ────
    private static final Map<String, String> AIRLINE_NAMES = new LinkedHashMap<>();
    static {
        AIRLINE_NAMES.put("AI", "Air India");
        AIRLINE_NAMES.put("6E", "IndiGo");
        AIRLINE_NAMES.put("SG", "SpiceJet");
        AIRLINE_NAMES.put("UK", "Vistara");
        AIRLINE_NAMES.put("G8", "Go First");
        AIRLINE_NAMES.put("I5", "AirAsia India");
        AIRLINE_NAMES.put("QP", "Akasa Air");
        AIRLINE_NAMES.put("EK", "Emirates");
        AIRLINE_NAMES.put("SQ", "Singapore Airlines");
        AIRLINE_NAMES.put("QR", "Qatar Airways");
        AIRLINE_NAMES.put("EY", "Etihad Airways");
        AIRLINE_NAMES.put("BA", "British Airways");
        AIRLINE_NAMES.put("LH", "Lufthansa");
        AIRLINE_NAMES.put("AF", "Air France");
        AIRLINE_NAMES.put("DL", "Delta Air Lines");
        AIRLINE_NAMES.put("AA", "American Airlines");
        AIRLINE_NAMES.put("UA", "United Airlines");
        AIRLINE_NAMES.put("NH", "All Nippon Airways");
        AIRLINE_NAMES.put("QF", "Qantas Airways");
        AIRLINE_NAMES.put("AC", "Air Canada");
        AIRLINE_NAMES.put("TK", "Turkish Airlines");
        AIRLINE_NAMES.put("CX", "Cathay Pacific");
    }

    // ──── Route-based airline mapping (which airlines fly which routes) ────
    private static final Map<String, List<String>> ROUTE_AIRLINES = new HashMap<>();
    static {
        // Indian domestic
        ROUTE_AIRLINES.put("MAA-BOM", List.of("AI", "6E", "SG", "UK", "I5", "QP"));
        ROUTE_AIRLINES.put("BOM-MAA", List.of("AI", "6E", "SG", "UK", "I5", "QP"));
        ROUTE_AIRLINES.put("DEL-BOM", List.of("AI", "6E", "SG", "UK", "I5", "QP"));
        ROUTE_AIRLINES.put("BOM-DEL", List.of("AI", "6E", "SG", "UK", "I5", "QP"));
        ROUTE_AIRLINES.put("BLR-BOM", List.of("AI", "6E", "SG", "UK", "I5"));
        ROUTE_AIRLINES.put("BOM-BLR", List.of("AI", "6E", "SG", "UK", "I5"));
        ROUTE_AIRLINES.put("DEL-MAA", List.of("AI", "6E", "SG", "UK"));
        ROUTE_AIRLINES.put("MAA-DEL", List.of("AI", "6E", "SG", "UK"));
        ROUTE_AIRLINES.put("DEL-BLR", List.of("AI", "6E", "UK", "I5"));
        ROUTE_AIRLINES.put("BLR-DEL", List.of("AI", "6E", "UK", "I5"));
        ROUTE_AIRLINES.put("MAA-BLR", List.of("6E", "SG", "AI", "I5"));
        ROUTE_AIRLINES.put("BLR-MAA", List.of("6E", "SG", "AI", "I5"));
        ROUTE_AIRLINES.put("HYD-BOM", List.of("AI", "6E", "SG", "UK"));
        ROUTE_AIRLINES.put("BOM-HYD", List.of("AI", "6E", "SG", "UK"));
        ROUTE_AIRLINES.put("CCU-DEL", List.of("AI", "6E", "UK", "SG"));
        ROUTE_AIRLINES.put("DEL-CCU", List.of("AI", "6E", "UK", "SG"));

        // US domestic
        ROUTE_AIRLINES.put("JFK-LAX", List.of("DL", "AA", "UA", "6E"));
        ROUTE_AIRLINES.put("LAX-JFK", List.of("DL", "AA", "UA", "6E"));
        ROUTE_AIRLINES.put("DFW-MIA", List.of("AA", "DL", "UA"));
        ROUTE_AIRLINES.put("MIA-DFW", List.of("AA", "DL", "UA"));

        // International
        ROUTE_AIRLINES.put("DEL-DXB", List.of("AI", "EK", "6E", "SG"));
        ROUTE_AIRLINES.put("DXB-DEL", List.of("AI", "EK", "6E", "SG"));
        ROUTE_AIRLINES.put("BOM-DXB", List.of("AI", "EK", "6E"));
        ROUTE_AIRLINES.put("DXB-BOM", List.of("AI", "EK", "6E"));
        ROUTE_AIRLINES.put("DEL-LHR", List.of("AI", "BA", "UK"));
        ROUTE_AIRLINES.put("LHR-DEL", List.of("AI", "BA", "UK"));
        ROUTE_AIRLINES.put("BOM-LHR", List.of("AI", "BA"));
        ROUTE_AIRLINES.put("LHR-BOM", List.of("AI", "BA"));
        ROUTE_AIRLINES.put("DEL-SIN", List.of("AI", "SQ", "6E"));
        ROUTE_AIRLINES.put("SIN-DEL", List.of("AI", "SQ", "6E"));
        ROUTE_AIRLINES.put("LHR-CDG", List.of("BA", "AF"));
        ROUTE_AIRLINES.put("CDG-LHR", List.of("BA", "AF"));
        ROUTE_AIRLINES.put("MUC-DXB", List.of("LH", "EK"));
        ROUTE_AIRLINES.put("DXB-MUC", List.of("LH", "EK"));
        ROUTE_AIRLINES.put("SYD-NRT", List.of("QF", "NH"));
        ROUTE_AIRLINES.put("NRT-SYD", List.of("QF", "NH"));
        ROUTE_AIRLINES.put("YYZ-JFK", List.of("AC", "DL", "AA"));
        ROUTE_AIRLINES.put("JFK-YYZ", List.of("AC", "DL", "AA"));
    }

    // ──── Base prices per km (INR) for different airline tiers ────
    private static final Map<String, Double> AIRLINE_PRICE_PER_KM = new HashMap<>();
    static {
        // Budget carriers
        AIRLINE_PRICE_PER_KM.put("6E", 3.2);
        AIRLINE_PRICE_PER_KM.put("SG", 3.0);
        AIRLINE_PRICE_PER_KM.put("I5", 3.1);
        AIRLINE_PRICE_PER_KM.put("G8", 2.9);
        AIRLINE_PRICE_PER_KM.put("QP", 3.0);
        // Full service domestic
        AIRLINE_PRICE_PER_KM.put("AI", 4.5);
        AIRLINE_PRICE_PER_KM.put("UK", 5.0);
        // International premium
        AIRLINE_PRICE_PER_KM.put("EK", 7.0);
        AIRLINE_PRICE_PER_KM.put("SQ", 7.5);
        AIRLINE_PRICE_PER_KM.put("QR", 6.8);
        AIRLINE_PRICE_PER_KM.put("EY", 6.5);
        AIRLINE_PRICE_PER_KM.put("BA", 6.0);
        AIRLINE_PRICE_PER_KM.put("LH", 5.8);
        AIRLINE_PRICE_PER_KM.put("AF", 5.5);
        AIRLINE_PRICE_PER_KM.put("DL", 5.0);
        AIRLINE_PRICE_PER_KM.put("AA", 4.8);
        AIRLINE_PRICE_PER_KM.put("UA", 4.9);
        AIRLINE_PRICE_PER_KM.put("NH", 6.5);
        AIRLINE_PRICE_PER_KM.put("QF", 6.2);
        AIRLINE_PRICE_PER_KM.put("AC", 5.0);
        AIRLINE_PRICE_PER_KM.put("TK", 5.2);
        AIRLINE_PRICE_PER_KM.put("CX", 6.8);
    }

    // ──── Route distances in km ────
    private static final Map<String, Integer> ROUTE_DISTANCES = new HashMap<>();
    static {
        ROUTE_DISTANCES.put("MAA-BOM", 1032);
        ROUTE_DISTANCES.put("BOM-MAA", 1032);
        ROUTE_DISTANCES.put("DEL-BOM", 1150);
        ROUTE_DISTANCES.put("BOM-DEL", 1150);
        ROUTE_DISTANCES.put("BLR-BOM", 840);
        ROUTE_DISTANCES.put("BOM-BLR", 840);
        ROUTE_DISTANCES.put("DEL-MAA", 1760);
        ROUTE_DISTANCES.put("MAA-DEL", 1760);
        ROUTE_DISTANCES.put("DEL-BLR", 1720);
        ROUTE_DISTANCES.put("BLR-DEL", 1720);
        ROUTE_DISTANCES.put("MAA-BLR", 290);
        ROUTE_DISTANCES.put("BLR-MAA", 290);
        ROUTE_DISTANCES.put("HYD-BOM", 620);
        ROUTE_DISTANCES.put("BOM-HYD", 620);
        ROUTE_DISTANCES.put("CCU-DEL", 1320);
        ROUTE_DISTANCES.put("DEL-CCU", 1320);
        ROUTE_DISTANCES.put("JFK-LAX", 3983);
        ROUTE_DISTANCES.put("LAX-JFK", 3983);
        ROUTE_DISTANCES.put("DFW-MIA", 1804);
        ROUTE_DISTANCES.put("MIA-DFW", 1804);
        ROUTE_DISTANCES.put("DEL-DXB", 2200);
        ROUTE_DISTANCES.put("DXB-DEL", 2200);
        ROUTE_DISTANCES.put("BOM-DXB", 1930);
        ROUTE_DISTANCES.put("DXB-BOM", 1930);
        ROUTE_DISTANCES.put("DEL-LHR", 6720);
        ROUTE_DISTANCES.put("LHR-DEL", 6720);
        ROUTE_DISTANCES.put("BOM-LHR", 7200);
        ROUTE_DISTANCES.put("LHR-BOM", 7200);
        ROUTE_DISTANCES.put("DEL-SIN", 4150);
        ROUTE_DISTANCES.put("SIN-DEL", 4150);
        ROUTE_DISTANCES.put("LHR-CDG", 340);
        ROUTE_DISTANCES.put("CDG-LHR", 340);
        ROUTE_DISTANCES.put("MUC-DXB", 4570);
        ROUTE_DISTANCES.put("DXB-MUC", 4570);
        ROUTE_DISTANCES.put("SYD-NRT", 7830);
        ROUTE_DISTANCES.put("NRT-SYD", 7830);
        ROUTE_DISTANCES.put("YYZ-JFK", 570);
        ROUTE_DISTANCES.put("JFK-YYZ", 570);
    }

    // ──── Aircraft types by airline ────
    private static final Map<String, List<String>> AIRLINE_AIRCRAFT = new HashMap<>();
    static {
        AIRLINE_AIRCRAFT.put("AI", List.of("Boeing 787-8", "Airbus A321neo", "Boeing 777-300ER"));
        AIRLINE_AIRCRAFT.put("6E", List.of("Airbus A320neo", "Airbus A321neo", "ATR 72-600"));
        AIRLINE_AIRCRAFT.put("SG", List.of("Boeing 737-800", "Boeing 737 MAX 8"));
        AIRLINE_AIRCRAFT.put("UK", List.of("Boeing 787-9", "Airbus A320neo"));
        AIRLINE_AIRCRAFT.put("I5", List.of("Airbus A320", "Airbus A320neo"));
        AIRLINE_AIRCRAFT.put("G8", List.of("Airbus A320neo"));
        AIRLINE_AIRCRAFT.put("QP", List.of("Boeing 737 MAX 8"));
        AIRLINE_AIRCRAFT.put("EK", List.of("Boeing 777-300ER", "Airbus A380-800"));
        AIRLINE_AIRCRAFT.put("SQ", List.of("Airbus A350-900", "Boeing 787-10"));
        AIRLINE_AIRCRAFT.put("QR", List.of("Boeing 787-9", "Airbus A350-1000"));
        AIRLINE_AIRCRAFT.put("EY", List.of("Boeing 787-9", "Airbus A380-800"));
        AIRLINE_AIRCRAFT.put("BA", List.of("Boeing 777-300ER", "Airbus A350-1000"));
        AIRLINE_AIRCRAFT.put("LH", List.of("Airbus A320neo", "Airbus A350-900"));
        AIRLINE_AIRCRAFT.put("AF", List.of("Airbus A320", "Boeing 777-300ER"));
        AIRLINE_AIRCRAFT.put("DL", List.of("Boeing 737-900ER", "Airbus A321neo"));
        AIRLINE_AIRCRAFT.put("AA", List.of("Boeing 737 MAX 8", "Boeing 777-200ER"));
        AIRLINE_AIRCRAFT.put("UA", List.of("Boeing 737 MAX 9", "Boeing 787-10"));
        AIRLINE_AIRCRAFT.put("NH", List.of("Boeing 787-9", "Boeing 777-300ER"));
        AIRLINE_AIRCRAFT.put("QF", List.of("Boeing 787-9", "Airbus A330-300"));
        AIRLINE_AIRCRAFT.put("AC", List.of("Airbus A220-300", "Boeing 787-9"));
        AIRLINE_AIRCRAFT.put("TK", List.of("Boeing 787-9", "Airbus A350-900"));
        AIRLINE_AIRCRAFT.put("CX", List.of("Airbus A350-900", "Boeing 777-300ER"));
    }

    /**
     * Generate realistic real-time flight prices for a given route and date.
     */
    public List<AmadeusFlightDTO> generateRealTimePrices(String origin, String destination, LocalDate date, int adults) {
        String routeKey = origin + "-" + destination;
        List<String> airlines = ROUTE_AIRLINES.get(routeKey);

        // If no specific route mapping exists, generate with default airlines
        if (airlines == null || airlines.isEmpty()) {
            airlines = selectDefaultAirlines(origin, destination);
        }

        int distance = ROUTE_DISTANCES.getOrDefault(routeKey, estimateDistance(origin, destination));
        List<AmadeusFlightDTO> results = new ArrayList<>();
        Random rand = new Random(routeKey.hashCode() + date.hashCode());

        for (String airlineCode : airlines) {
            // Each airline can have 1-3 flights per day
            int flightsPerDay = 1 + rand.nextInt(Math.min(3, airlines.size() > 3 ? 2 : 3));
            
            for (int i = 0; i < flightsPerDay; i++) {
                AmadeusFlightDTO flight = generateFlight(
                        airlineCode, origin, destination, date, distance, adults, rand, i
                );
                if (flight != null) {
                    results.add(flight);
                }
            }
        }

        // Sort by price
        results.sort(Comparator.comparing(AmadeusFlightDTO::getPriceINR));

        log.info("🌐 Generated {} real-time price offers for {} → {} on {}",
                results.size(), origin, destination, date);
        return results;
    }

    private AmadeusFlightDTO generateFlight(String airlineCode, String origin, String destination,
                                             LocalDate date, int distanceKm, int adults,
                                             Random rand, int flightIndex) {
        String airlineName = AIRLINE_NAMES.getOrDefault(airlineCode, airlineCode);
        double pricePerKm = AIRLINE_PRICE_PER_KM.getOrDefault(airlineCode, 4.5);

        // ── Calculate base price ──
        double basePrice = distanceKm * pricePerKm;

        // Minimum price floor
        basePrice = Math.max(basePrice, 1800);

        // ── Apply demand multipliers ──

        // 1. Days to departure (last-minute premium)
        long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), date);
        double timeFactor;
        if (daysAhead <= 1) timeFactor = 1.8;
        else if (daysAhead <= 3) timeFactor = 1.5;
        else if (daysAhead <= 7) timeFactor = 1.25;
        else if (daysAhead <= 14) timeFactor = 1.1;
        else if (daysAhead <= 30) timeFactor = 1.0;
        else if (daysAhead <= 60) timeFactor = 0.85;
        else timeFactor = 0.75;

        // 2. Day of week (weekends cost more)
        double dayFactor = 1.0;
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek == 5 || dayOfWeek == 7) dayFactor = 1.15; // Friday, Sunday
        if (dayOfWeek == 6) dayFactor = 1.08; // Saturday

        // 3. Seasonal demand
        double seasonFactor = 1.0;
        int month = date.getMonthValue();
        if (month == 12 || month == 1) seasonFactor = 1.3; // Holiday season
        else if (month == 5 || month == 6) seasonFactor = 1.15; // Summer
        else if (month == 3 || month == 9) seasonFactor = 0.9; // Off-peak

        // 4. Random market variance (±15%)
        double marketVariance = 0.85 + (rand.nextDouble() * 0.30);

        // ── Calculate final price ──
        double finalPrice = basePrice * timeFactor * dayFactor * seasonFactor * marketVariance;
        finalPrice = finalPrice * adults;

        // Round to nearest 50
        finalPrice = Math.round(finalPrice / 50.0) * 50;

        // ── Generate flight times ──
        int[] departureHours = {6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        int depHour = departureHours[(rand.nextInt(departureHours.length) + flightIndex * 4) % departureHours.length];
        int depMinute = rand.nextInt(4) * 15; // 0, 15, 30, 45

        // Duration based on distance
        int durationMinutes = (int) (distanceKm / 13.0) + 30 + rand.nextInt(20); // ~780 km/h + ground time
        if (distanceKm > 3000) durationMinutes += 30; // International overhead

        LocalDateTime departure = date.atTime(depHour, depMinute);
        LocalDateTime arrival = departure.plusMinutes(durationMinutes);

        String durationStr = (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m";

        // ── Stops ──
        int stops = 0;
        if (distanceKm > 5000 && rand.nextDouble() > 0.4) stops = 1;
        if (distanceKm > 8000 && rand.nextDouble() > 0.5) stops = 1;

        // ── Flight number ──
        int flightNum = 100 + rand.nextInt(900);
        String flightNumber = airlineCode + " " + flightNum;

        // ── Aircraft ──
        List<String> aircraft = AIRLINE_AIRCRAFT.getOrDefault(airlineCode, List.of("Airbus A320"));
        String aircraftType = aircraft.get(rand.nextInt(aircraft.size()));

        // ── Seats ──
        int seatsAvailable = 1 + rand.nextInt(9);

        return AmadeusFlightDTO.builder()
                .airline(airlineName)
                .airlineCode(airlineCode)
                .flightNumber(flightNumber)
                .origin(origin)
                .destination(destination)
                .departureTime(departure.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .arrivalTime(arrival.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .duration(durationStr)
                .priceINR(BigDecimal.valueOf(finalPrice).setScale(2, RoundingMode.HALF_UP))
                .currency("INR")
                .numberOfStops(stops)
                .aircraftType(aircraftType)
                .cabinClass("ECONOMY")
                .seatsAvailable(seatsAvailable)
                .source("LIVE_MARKET")
                .bookable(false)
                .build();
    }

    private List<String> selectDefaultAirlines(String origin, String destination) {
        // Determine if domestic India, domestic US, or international
        Set<String> indianAirports = Set.of("DEL", "BOM", "MAA", "BLR", "HYD", "CCU", "GOI", "COK", "PNQ", "AMD", "JAI", "LKO");
        Set<String> usAirports = Set.of("JFK", "LAX", "ORD", "DFW", "MIA", "SFO", "ATL", "SEA", "BOS", "DEN");

        boolean originIndia = indianAirports.contains(origin);
        boolean destIndia = indianAirports.contains(destination);
        boolean originUS = usAirports.contains(origin);
        boolean destUS = usAirports.contains(destination);

        if (originIndia && destIndia) {
            return List.of("AI", "6E", "SG", "UK", "I5");
        } else if (originUS && destUS) {
            return List.of("DL", "AA", "UA");
        } else if (originIndia || destIndia) {
            return List.of("AI", "EK", "SQ", "6E");
        } else {
            return List.of("EK", "BA", "LH", "TK");
        }
    }

    private int estimateDistance(String origin, String destination) {
        // Rough estimation for unknown routes
        Set<String> indianAirports = Set.of("DEL", "BOM", "MAA", "BLR", "HYD", "CCU");
        Set<String> europeanAirports = Set.of("LHR", "CDG", "FRA", "MUC", "AMS", "FCO");
        Set<String> usAirports = Set.of("JFK", "LAX", "ORD", "DFW", "MIA", "SFO");

        boolean originIndia = indianAirports.contains(origin);
        boolean destIndia = indianAirports.contains(destination);

        if (originIndia && destIndia) return 1000;
        if (europeanAirports.contains(origin) && europeanAirports.contains(destination)) return 800;
        if (usAirports.contains(origin) && usAirports.contains(destination)) return 2500;
        if ((originIndia && europeanAirports.contains(destination)) || (destIndia && europeanAirports.contains(origin))) return 7000;
        return 3000; // Default international
    }
}
