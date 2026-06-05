package com.airline.service;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.exceptions.ResponseException;
import com.amadeus.resources.FlightOfferSearch;
import com.airline.common.dto.AmadeusFlightDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service that integrates with the Amadeus Flight Offers Search API
 * to retrieve real-time flight prices from global airlines.
 */
@Service
@Slf4j
public class AmadeusFlightService {

    @Autowired
    private RealTimePriceEngine realTimePriceEngine;

    @Value("${amadeus.api-key:}")
    private String apiKey;

    @Value("${amadeus.api-secret:}")
    private String apiSecret;

    @Value("${amadeus.environment:test}")
    private String environment;

    private Amadeus amadeus;
    private boolean configured = false;

    // Approximate USD to INR conversion rate
    private static final BigDecimal USD_TO_INR = new BigDecimal("83.50");
    private static final BigDecimal EUR_TO_INR = new BigDecimal("91.00");
    private static final BigDecimal GBP_TO_INR = new BigDecimal("106.00");

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            try {
                if ("production".equalsIgnoreCase(environment)) {
                    amadeus = Amadeus
                            .builder(apiKey, apiSecret)
                            .setHostname("production")
                            .build();
                } else {
                    amadeus = Amadeus
                            .builder(apiKey, apiSecret)
                            .build();
                }
                configured = true;
                log.info("✅ Amadeus Flight API initialized successfully (environment: {})", environment);
            } catch (Exception e) {
                log.warn("⚠️ Failed to initialize Amadeus API: {}. Real-time prices will be unavailable.", e.getMessage());
                configured = false;
            }
        } else {
            log.info("ℹ️ Amadeus API credentials not configured. Set AMADEUS_API_KEY and AMADEUS_API_SECRET to enable real-time flight prices.");
            configured = false;
        }
    }

    /**
     * Check if the Amadeus API is properly configured and available.
     */
    public boolean isAvailable() {
        return configured && amadeus != null;
    }

    /**
     * Search for real-time flight offers from the Amadeus API.
     *
     * @param origin      IATA airport code (e.g., "MAA", "JFK")
     * @param destination IATA airport code (e.g., "BOM", "LAX")
     * @param date        departure date
     * @param adults      number of adult passengers
     * @return list of real-time flight offers, or empty list if API is unavailable
     */
    public List<AmadeusFlightDTO> searchRealTimeFlights(String origin, String destination, LocalDate date, int adults) {
        String resolvedOrigin = com.airline.common.util.AirportResolver.resolveToIata(origin);
        String resolvedDestination = com.airline.common.util.AirportResolver.resolveToIata(destination);

        if (!isAvailable()) {
            log.info("ℹ️ Amadeus API not available. Generating simulated real-time prices for {} → {} on {}", resolvedOrigin, resolvedDestination, date);
            return realTimePriceEngine.generateRealTimePrices(resolvedOrigin, resolvedDestination, date, adults);
        }

        try {
            log.info("🔍 Searching Amadeus for flights: {} → {} on {} ({} adults)",
                    resolvedOrigin, resolvedDestination, date, adults);

            FlightOfferSearch[] offers = amadeus.shopping.flightOffersSearch.get(
                    Params.with("originLocationCode", resolvedOrigin.toUpperCase())
                            .and("destinationLocationCode", resolvedDestination.toUpperCase())
                            .and("departureDate", date.toString())
                            .and("adults", adults)
                            .and("currencyCode", "INR")
                            .and("max", 10)
            );

            List<AmadeusFlightDTO> results = new ArrayList<>();
            for (FlightOfferSearch offer : offers) {
                try {
                    AmadeusFlightDTO dto = mapOfferToDTO(offer);
                    if (dto != null) {
                        results.add(dto);
                    }
                } catch (Exception e) {
                    log.warn("Failed to map flight offer: {}", e.getMessage());
                }
            }

            log.info("✅ Amadeus returned {} flight offers for {} → {}", results.size(), resolvedOrigin, resolvedDestination);
            return results;

        } catch (ResponseException e) {
            log.error("❌ Amadeus API error: {} (status: {}). Falling back to simulated prices.", e.getMessage(), e.getCode());
            return realTimePriceEngine.generateRealTimePrices(resolvedOrigin, resolvedDestination, date, adults);
        } catch (Exception e) {
            log.error("❌ Unexpected error calling Amadeus API: {}. Falling back to simulated prices.", e.getMessage());
            return realTimePriceEngine.generateRealTimePrices(resolvedOrigin, resolvedDestination, date, adults);
        }
    }

    /**
     * Map an Amadeus FlightOfferSearch to our internal DTO.
     */
    private AmadeusFlightDTO mapOfferToDTO(FlightOfferSearch offer) {
        if (offer == null || offer.getItineraries() == null || offer.getItineraries().length == 0) {
            return null;
        }

        FlightOfferSearch.Itinerary itinerary = offer.getItineraries()[0];
        FlightOfferSearch.SearchSegment[] segments = itinerary.getSegments();

        if (segments == null || segments.length == 0) {
            return null;
        }

        FlightOfferSearch.SearchSegment firstSegment = segments[0];
        FlightOfferSearch.SearchSegment lastSegment = segments[segments.length - 1];

        // Extract price
        BigDecimal price = BigDecimal.ZERO;
        String currency = "INR";
        if (offer.getPrice() != null) {
            try {
                price = new BigDecimal(offer.getPrice().getTotal());
                currency = offer.getPrice().getCurrency();
                // Convert to INR if needed
                price = convertToINR(price, currency);
                currency = "INR";
            } catch (NumberFormatException e) {
                log.warn("Could not parse price: {}", offer.getPrice().getTotal());
            }
        }

        // Extract aircraft type
        String aircraftType = "Unknown";
        if (firstSegment.getAircraft() != null && firstSegment.getAircraft().getCode() != null) {
            aircraftType = firstSegment.getAircraft().getCode();
        }

        // Extract cabin class
        String cabinClass = "ECONOMY";
        if (offer.getTravelerPricings() != null && offer.getTravelerPricings().length > 0) {
            FlightOfferSearch.TravelerPricing tp = offer.getTravelerPricings()[0];
            if (tp.getFareDetailsBySegment() != null && tp.getFareDetailsBySegment().length > 0) {
                String cabin = tp.getFareDetailsBySegment()[0].getCabin();
                if (cabin != null) {
                    cabinClass = cabin;
                }
            }
        }

        // Seats available
        int seatsAvailable = 9; // Default
        if (offer.getNumberOfBookableSeats() != 0) {
            seatsAvailable = offer.getNumberOfBookableSeats();
        }

        // Build flight number
        String carrierCode = firstSegment.getCarrierCode() != null ? firstSegment.getCarrierCode() : "XX";
        String flightNum = firstSegment.getNumber() != null ? firstSegment.getNumber() : "0000";

        return AmadeusFlightDTO.builder()
                .airline(carrierCode) // Carrier code (e.g., "AI", "EK", "6E")
                .airlineCode(carrierCode)
                .flightNumber(carrierCode + " " + flightNum)
                .origin(firstSegment.getDeparture().getIataCode())
                .destination(lastSegment.getArrival().getIataCode())
                .departureTime(firstSegment.getDeparture().getAt())
                .arrivalTime(lastSegment.getArrival().getAt())
                .duration(itinerary.getDuration() != null ? formatDuration(itinerary.getDuration()) : "N/A")
                .priceINR(price.setScale(2, RoundingMode.HALF_UP))
                .currency(currency)
                .numberOfStops(segments.length - 1)
                .aircraftType(aircraftType)
                .cabinClass(cabinClass)
                .seatsAvailable(seatsAvailable)
                .source("AMADEUS")
                .bookable(false) // External flights can't be booked in our system
                .build();
    }

    /**
     * Convert price to INR based on source currency.
     */
    private BigDecimal convertToINR(BigDecimal amount, String currency) {
        if (currency == null || "INR".equalsIgnoreCase(currency)) {
            return amount;
        }
        return switch (currency.toUpperCase()) {
            case "USD" -> amount.multiply(USD_TO_INR);
            case "EUR" -> amount.multiply(EUR_TO_INR);
            case "GBP" -> amount.multiply(GBP_TO_INR);
            default -> amount.multiply(USD_TO_INR); // Default to USD conversion
        };
    }

    /**
     * Format ISO 8601 duration (e.g., "PT5H30M") to human-readable format (e.g., "5h 30m").
     */
    private String formatDuration(String isoDuration) {
        if (isoDuration == null) return "N/A";
        try {
            String cleaned = isoDuration.replace("PT", "");
            StringBuilder sb = new StringBuilder();
            if (cleaned.contains("H")) {
                String[] parts = cleaned.split("H");
                sb.append(parts[0]).append("h ");
                if (parts.length > 1 && parts[1].contains("M")) {
                    sb.append(parts[1].replace("M", "")).append("m");
                }
            } else if (cleaned.contains("M")) {
                sb.append(cleaned.replace("M", "")).append("m");
            } else {
                return isoDuration;
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return isoDuration;
        }
    }
}
