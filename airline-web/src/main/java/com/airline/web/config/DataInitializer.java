package com.airline.web.config;

import com.airline.common.model.FlightStatus;
import com.airline.common.model.RoleName;
import com.airline.domain.entity.*;
import com.airline.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AirlineRepository airlineRepository;
    private final AircraftRepository aircraftRepository;
    private final RouteRepository routeRepository;
    private final FlightRepository flightRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting AeroNexus database seeding with top 15 countries and top 15 states/regions...");

        // 1. Seed Roles
        Role customerRole = seedRole(RoleName.ROLE_CUSTOMER);
        Role staffRole = seedRole(RoleName.ROLE_STAFF);
        Role adminRole = seedRole(RoleName.ROLE_ADMIN);

        // 2. Seed Users
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@aeronexus.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .roles(new HashSet<>(List.of(adminRole, staffRole)))
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded Admin user: admin / admin123");
        }

        if (!userRepository.existsByUsername("customer")) {
            User customer = User.builder()
                    .username("customer")
                    .email("customer@example.com")
                    .password(passwordEncoder.encode("customer123"))
                    .firstName("John")
                    .lastName("Doe")
                    .roles(new HashSet<>(Collections.singletonList(customerRole)))
                    .enabled(true)
                    .build();
            userRepository.save(customer);
            log.info("Seeded Customer user: customer / customer123");
        }

        // 3. Seed 15 Airlines representing the Top 15 Countries
        Airline delta = seedAirline("Delta Air Lines", "DL", "United States");
        Airline airIndia = seedAirline("Air India", "AI", "India");
        Airline britishAirways = seedAirline("British Airways", "BA", "United Kingdom");
        Airline lufthansa = seedAirline("Lufthansa", "LH", "Germany");
        Airline airFrance = seedAirline("Air France", "AF", "France");
        Airline ana = seedAirline("All Nippon Airways", "NH", "Japan");
        Airline airCanada = seedAirline("Air Canada", "AC", "Canada");
        Airline qantas = seedAirline("Qantas Airways", "QF", "Australia");
        Airline singaporeAir = seedAirline("Singapore Airlines", "SQ", "Singapore");
        Airline emirates = seedAirline("Emirates", "EK", "United Arab Emirates");
        Airline latam = seedAirline("LATAM Brasil", "LA", "Brazil");
        Airline saa = seedAirline("South African Airways", "SA", "South Africa");
        Airline airChina = seedAirline("Air China", "CA", "China");
        Airline iberia = seedAirline("Iberia", "IB", "Spain");
        Airline ita = seedAirline("ITA Airways", "AZ", "Italy");

        // 4. Seed 15 Aircraft for these Airlines (Top 15 Countries)
        Aircraft ac1 = seedAircraft("Boeing 737 Max 9", "N-DL737", 180, delta);
        Aircraft ac2 = seedAircraft("Boeing 787-8", "VT-AI787", 256, airIndia);
        Aircraft ac3 = seedAircraft("Boeing 777-300ER", "G-BA777", 299, britishAirways);
        Aircraft ac4 = seedAircraft("Airbus A320neo", "D-LH320", 168, lufthansa);
        Aircraft ac5 = seedAircraft("Airbus A350-900", "F-AF350", 324, airFrance);
        Aircraft ac6 = seedAircraft("Boeing 787-9", "JA-NH787", 246, ana);
        Aircraft ac7 = seedAircraft("Airbus A220-300", "C-AC220", 137, airCanada);
        Aircraft ac8 = seedAircraft("Boeing 787-9", "VH-QF787", 236, qantas);
        Aircraft ac9 = seedAircraft("Airbus A380-800", "9V-SQ380", 471, singaporeAir);
        Aircraft ac10 = seedAircraft("Airbus A380-800", "A6-EK380", 489, emirates);
        Aircraft ac11 = seedAircraft("Airbus A321neo", "PR-LA321", 224, latam);
        Aircraft ac12 = seedAircraft("Airbus A330-300", "ZS-SA330", 249, saa);
        Aircraft ac13 = seedAircraft("Boeing 777-300ER", "B-CA777", 311, airChina);
        Aircraft ac14 = seedAircraft("Airbus A320neo", "EC-IB320", 186, iberia);
        Aircraft ac15 = seedAircraft("Airbus A320neo", "EI-AZ320", 174, ita);

        // 5. Seed Routes representing Top 15 States/Provinces/Regions
        // We will seed both standard IATA airport codes and state names as origins/destinations.
        
        // State 1 & 2: New York (JFK) <-> California (LAX)
        seedRoute("JFK", "LAX", 2475, 360);
        seedRoute("LAX", "JFK", 2475, 360);
        seedRoute("NEW YORK", "CALIFORNIA", 2475, 360);
        seedRoute("CALIFORNIA", "NEW YORK", 2475, 360);

        // State 3 & 4: Texas (DFW) <-> Florida (MIA)
        seedRoute("DFW", "MIA", 1121, 170);
        seedRoute("MIA", "DFW", 1121, 170);
        seedRoute("TEXAS", "FLORIDA", 1121, 170);
        seedRoute("FLORIDA", "TEXAS", 1121, 170);

        // State 5: Tamil Nadu (MAA) <-> Maharashtra (BOM)
        seedRoute("MAA", "BOM", 640, 110);
        seedRoute("BOM", "MAA", 640, 110);
        seedRoute("TAMIL NADU", "MAHARASHTRA", 640, 110);
        seedRoute("MAHARASHTRA", "TAMIL NADU", 640, 110);
        seedRoute("CHENNAI", "MAHARASHTRA", 640, 110);
        seedRoute("MAHARASHTRA", "CHENNAI", 640, 110);

        // State 6 & 7: Delhi State (DEL) <-> Maharashtra (BOM)
        seedRoute("DEL", "BOM", 715, 120);
        seedRoute("BOM", "DEL", 715, 120);
        seedRoute("DELHI", "MAHARASHTRA", 715, 120);
        seedRoute("MAHARASHTRA", "DELHI", 715, 120);

        // State 8: Karnataka (BLR) <-> Maharashtra (BOM)
        seedRoute("BLR", "BOM", 520, 95);
        seedRoute("BOM", "BLR", 520, 95);
        seedRoute("KARNATAKA", "MAHARASHTRA", 520, 95);
        seedRoute("MAHARASHTRA", "KARNATAKA", 520, 95);

        // State 9: Ontario (YYZ) <-> New York (JFK)
        seedRoute("YYZ", "JFK", 366, 90);
        seedRoute("JFK", "YYZ", 366, 90);
        seedRoute("ONTARIO", "NEW YORK", 366, 90);
        seedRoute("NEW YORK", "ONTARIO", 366, 90);

        // State 10 & 11: New South Wales (SYD) <-> Tokyo (NRT)
        seedRoute("SYD", "NRT", 4840, 580);
        seedRoute("NRT", "SYD", 4840, 580);
        seedRoute("NEW SOUTH WALES", "TOKYO", 4840, 580);
        seedRoute("TOKYO", "NEW SOUTH WALES", 4840, 580);

        // State 12 & 13: England (LHR) <-> Île-de-France (CDG)
        seedRoute("LHR", "CDG", 216, 75);
        seedRoute("CDG", "LHR", 216, 75);
        seedRoute("ENGLAND", "ILE-DE-FRANCE", 216, 75);
        seedRoute("ILE-DE-FRANCE", "ENGLAND", 216, 75);

        // State 14 & 15: Bavaria (MUC) <-> Dubai (DXB)
        seedRoute("MUC", "DXB", 2840, 350);
        seedRoute("DXB", "MUC", 2840, 350);
        seedRoute("BAVARIA", "DUBAI", 2840, 350);
        seedRoute("DUBAI", "BAVARIA", 2840, 350);

        // 6. Seed Flights for all these routes
        List<Aircraft> acs = List.of(ac1, ac2, ac3, ac4, ac5, ac6, ac7, ac8, ac9, ac10, ac11, ac12, ac13, ac14, ac15);
        LocalDate start = LocalDate.now();
        for (int i = 0; i < 25; i++) {
            seedAllFlightsForDate(start.plusDays(i), acs);
        }
        
        // Also ensure targetDate 2026-06-06 is explicitly seeded if needed
        LocalDate targetDate = LocalDate.of(2026, 6, 6);
        seedAllFlightsForDate(targetDate, acs);

        log.info("AeroNexus Database seeding completed successfully!");
    }

    private Role seedRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = Role.builder().name(name).build();
            Role saved = roleRepository.save(r);
            log.info("Seeded role: {}", name);
            return saved;
        });
    }

    private Airline seedAirline(String name, String code, String country) {
        return airlineRepository.findByCode(code).orElseGet(() -> {
            Airline a = Airline.builder().name(name).code(code).country(country).build();
            Airline saved = airlineRepository.save(a);
            log.info("Seeded airline: {} ({})", name, country);
            return saved;
        });
    }

    private Aircraft seedAircraft(String model, String tailNumber, int capacity, Airline airline) {
        return aircraftRepository.findByTailNumber(tailNumber).orElseGet(() -> {
            Aircraft a = Aircraft.builder()
                    .model(model)
                    .tailNumber(tailNumber)
                    .capacity(capacity)
                    .airline(airline)
                    .status("ACTIVE")
                    .build();
            Aircraft saved = aircraftRepository.save(a);
            log.info("Seeded aircraft: {} (Tail: {}, Airline: {})", model, tailNumber, airline.getName());
            return saved;
        });
    }

    private Route seedRoute(String origin, String destination, int distance, int duration) {
        return routeRepository.findByOriginAndDestination(origin, destination).orElseGet(() -> {
            Route r = Route.builder()
                    .origin(origin)
                    .destination(destination)
                    .distanceMiles(distance)
                    .durationMinutes(duration)
                    .build();
            Route saved = routeRepository.save(r);
            log.info("Seeded route: {} ➔ {}", origin, destination);
            return saved;
        });
    }

    private void seedFlight(String flightNoPrefix, String origin, String destination, Aircraft aircraft, LocalDate date, int depHour, int depMinute, int durationMinutes, BigDecimal price) {
        String flightNumber = flightNoPrefix + "-" + date.toString();
        routeRepository.findByOriginAndDestination(origin, destination).ifPresent(route -> {
            LocalDateTime departure = date.atTime(depHour, depMinute);
            LocalDateTime arrival = departure.plusMinutes(durationMinutes);
            createFlightIfNotExists(flightNumber, route, aircraft, departure, arrival, price);
        });
    }

    private void seedAllFlightsForDate(LocalDate date, List<Aircraft> acs) {
        Aircraft deltaAc = acs.get(0);      // Delta (US)
        Aircraft airIndiaAc = acs.get(1);   // Air India (India)
        Aircraft baAc = acs.get(2);         // British Airways (UK)
        Aircraft lhAc = acs.get(3);         // Lufthansa (Germany)
        Aircraft afAc = acs.get(4);         // Air France (France)
        Aircraft anaAc = acs.get(5);        // ANA (Japan)
        Aircraft acCanada = acs.get(6);     // Air Canada (Canada)
        Aircraft qantasAc = acs.get(7);     // Qantas (Australia)
        Aircraft sqAc = acs.get(8);         // Singapore Airlines (Singapore)
        Aircraft emiratesAc = acs.get(9);   // Emirates (UAE)

        // 1. New York <-> California
        seedFlight("DL-101", "JFK", "LAX", deltaAc, date, 8, 0, 360, new BigDecimal("350.00"));
        seedFlight("DL-102", "LAX", "JFK", deltaAc, date, 15, 30, 360, new BigDecimal("380.00"));
        seedFlight("DL-101S", "NEW YORK", "CALIFORNIA", deltaAc, date, 8, 0, 360, new BigDecimal("350.00"));
        seedFlight("DL-102S", "CALIFORNIA", "NEW YORK", deltaAc, date, 15, 30, 360, new BigDecimal("380.00"));

        // 2. Texas <-> Florida
        seedFlight("AA-201", "DFW", "MIA", deltaAc, date, 9, 0, 170, new BigDecimal("199.00"));
        seedFlight("AA-202", "MIA", "DFW", deltaAc, date, 13, 0, 170, new BigDecimal("220.00"));
        seedFlight("AA-201S", "TEXAS", "FLORIDA", deltaAc, date, 9, 0, 170, new BigDecimal("199.00"));
        seedFlight("AA-202S", "FLORIDA", "TEXAS", deltaAc, date, 13, 0, 170, new BigDecimal("220.00"));

        // 3. Tamil Nadu <-> Maharashtra (Multiple timings: Morning, Mid-day, Evening)
        // Mid-day flights
        seedFlight("AI-301", "MAA", "BOM", airIndiaAc, date, 11, 15, 110, new BigDecimal("3500.00"));
        seedFlight("AI-302", "BOM", "MAA", airIndiaAc, date, 16, 0, 110, new BigDecimal("3600.00"));
        seedFlight("AI-301S", "TAMIL NADU", "MAHARASHTRA", airIndiaAc, date, 11, 15, 110, new BigDecimal("3500.00"));
        seedFlight("AI-302S", "MAHARASHTRA", "TAMIL NADU", airIndiaAc, date, 16, 0, 110, new BigDecimal("3600.00"));
        seedFlight("AI-301C", "CHENNAI", "MAHARASHTRA", airIndiaAc, date, 11, 15, 110, new BigDecimal("3500.00"));
        seedFlight("AI-302C", "MAHARASHTRA", "CHENNAI", airIndiaAc, date, 16, 0, 110, new BigDecimal("3600.00"));

        // Morning flights
        seedFlight("AI-303", "MAA", "BOM", airIndiaAc, date, 7, 30, 110, new BigDecimal("3300.00"));
        seedFlight("AI-304", "BOM", "MAA", airIndiaAc, date, 9, 15, 110, new BigDecimal("3400.00"));
        seedFlight("AI-303S", "TAMIL NADU", "MAHARASHTRA", airIndiaAc, date, 7, 30, 110, new BigDecimal("3300.00"));
        seedFlight("AI-304S", "MAHARASHTRA", "TAMIL NADU", airIndiaAc, date, 9, 15, 110, new BigDecimal("3400.00"));
        seedFlight("AI-303C", "CHENNAI", "MAHARASHTRA", airIndiaAc, date, 7, 30, 110, new BigDecimal("3300.00"));
        seedFlight("AI-304C", "MAHARASHTRA", "CHENNAI", airIndiaAc, date, 9, 15, 110, new BigDecimal("3400.00"));

        // Evening flights
        seedFlight("AI-305", "MAA", "BOM", airIndiaAc, date, 18, 45, 110, new BigDecimal("3700.00"));
        seedFlight("AI-306", "BOM", "MAA", airIndiaAc, date, 20, 30, 110, new BigDecimal("3800.00"));
        seedFlight("AI-305S", "TAMIL NADU", "MAHARASHTRA", airIndiaAc, date, 18, 45, 110, new BigDecimal("3700.00"));
        seedFlight("AI-306S", "MAHARASHTRA", "TAMIL NADU", airIndiaAc, date, 20, 30, 110, new BigDecimal("3800.00"));
        seedFlight("AI-305C", "CHENNAI", "MAHARASHTRA", airIndiaAc, date, 18, 45, 110, new BigDecimal("3700.00"));
        seedFlight("AI-306C", "MAHARASHTRA", "CHENNAI", airIndiaAc, date, 20, 30, 110, new BigDecimal("3800.00"));

        // 4. Delhi <-> Maharashtra
        seedFlight("AI-401", "DEL", "BOM", airIndiaAc, date, 7, 0, 120, new BigDecimal("4500.00"));
        seedFlight("AI-402", "BOM", "DEL", airIndiaAc, date, 10, 30, 120, new BigDecimal("4700.00"));
        seedFlight("AI-401S", "DELHI", "MAHARASHTRA", airIndiaAc, date, 7, 0, 120, new BigDecimal("4500.00"));
        seedFlight("AI-402S", "MAHARASHTRA", "DELHI", airIndiaAc, date, 10, 30, 120, new BigDecimal("4700.00"));

        // 5. Karnataka <-> Maharashtra
        seedFlight("AI-501", "BLR", "BOM", airIndiaAc, date, 14, 0, 95, new BigDecimal("2500.00"));
        seedFlight("AI-502", "BOM", "BLR", airIndiaAc, date, 17, 30, 95, new BigDecimal("2600.00"));
        seedFlight("AI-501S", "KARNATAKA", "MAHARASHTRA", airIndiaAc, date, 14, 0, 95, new BigDecimal("2500.00"));
        seedFlight("AI-502S", "MAHARASHTRA", "KARNATAKA", airIndiaAc, date, 17, 30, 95, new BigDecimal("2600.00"));

        // 6. Ontario <-> New York
        seedFlight("AC-601", "YYZ", "JFK", acCanada, date, 9, 30, 90, new BigDecimal("210.00"));
        seedFlight("AC-602", "JFK", "YYZ", acCanada, date, 12, 30, 90, new BigDecimal("230.00"));
        seedFlight("AC-601S", "ONTARIO", "NEW YORK", acCanada, date, 9, 30, 90, new BigDecimal("210.00"));
        seedFlight("AC-602S", "NEW YORK", "ONTARIO", acCanada, date, 12, 30, 90, new BigDecimal("230.00"));

        // 7. New South Wales <-> Tokyo
        seedFlight("QF-701", "SYD", "NRT", qantasAc, date, 22, 0, 580, new BigDecimal("850.00"));
        seedFlight("QF-702", "NRT", "SYD", qantasAc, date, 10, 0, 580, new BigDecimal("890.00"));
        seedFlight("QF-701S", "NEW SOUTH WALES", "TOKYO", qantasAc, date, 22, 0, 580, new BigDecimal("850.00"));
        seedFlight("QF-702S", "TOKYO", "NEW SOUTH WALES", qantasAc, date, 10, 0, 580, new BigDecimal("890.00"));

        // 8. England <-> Île-de-France
        seedFlight("BA-801", "LHR", "CDG", baAc, date, 8, 30, 75, new BigDecimal("89.00"));
        seedFlight("BA-802", "CDG", "LHR", baAc, date, 11, 0, 75, new BigDecimal("99.00"));
        seedFlight("BA-801S", "ENGLAND", "ILE-DE-FRANCE", baAc, date, 8, 30, 75, new BigDecimal("89.00"));
        seedFlight("BA-802S", "ILE-DE-FRANCE", "ENGLAND", baAc, date, 11, 0, 75, new BigDecimal("99.00"));

        // 9. Bavaria <-> Dubai
        seedFlight("LH-901", "MUC", "DXB", lhAc, date, 21, 30, 350, new BigDecimal("450.00"));
        seedFlight("LH-902", "DXB", "MUC", lhAc, date, 8, 30, 350, new BigDecimal("480.00"));
        seedFlight("LH-901S", "BAVARIA", "DUBAI", lhAc, date, 21, 30, 350, new BigDecimal("450.00"));
        seedFlight("LH-902S", "DUBAI", "BAVARIA", lhAc, date, 8, 30, 350, new BigDecimal("480.00"));
    }

    private void createFlightIfNotExists(String flightNumber, Route route, Aircraft aircraft,
                                         LocalDateTime departure, LocalDateTime arrival, BigDecimal basePrice) {
        if (flightRepository.findByFlightNumber(flightNumber).isEmpty()) {
            Flight f = Flight.builder()
                    .flightNumber(flightNumber)
                    .route(route)
                    .aircraft(aircraft)
                    .departureTime(departure)
                    .arrivalTime(arrival)
                    .basePrice(basePrice)
                    .status(FlightStatus.SCHEDULED)
                    .build();
            flightRepository.save(f);
            log.info("Seeded flight: {} ({}) on {}", flightNumber, route.getOrigin() + " ➔ " + route.getDestination(), departure.toLocalDate());
        }
    }
}
