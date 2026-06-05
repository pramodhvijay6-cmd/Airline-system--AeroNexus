package com.airline.service;

import com.airline.common.exception.ResourceNotFoundException;
import com.airline.common.model.BaggageStatus;
import com.airline.domain.entity.Baggage;
import com.airline.domain.entity.Passenger;
import com.airline.domain.repository.BaggageRepository;
import com.airline.domain.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaggageService {

    private final BaggageRepository baggageRepository;
    private final PassengerRepository passengerRepository;

    /**
     * Checks in a piece of baggage for a traveler.
     *
     * @param passengerId traveler database ID
     * @param bagTag      unique luggage tracking barcode tag
     * @param weightKg    measured weight of the bag
     * @return saved Baggage entity
     */
    @Transactional
    public Baggage checkInBaggage(Long passengerId, String bagTag, BigDecimal weightKg) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + passengerId));

        Baggage baggage = Baggage.builder()
                .passenger(passenger)
                .bagTag(bagTag)
                .weightKg(weightKg)
                .status(BaggageStatus.CHECKED_IN)
                .build();

        log.info("Checked in baggage tag: {} for passenger: {}", bagTag, passengerId);
        return baggageRepository.save(baggage);
    }

    /**
     * Updates transit location status of a piece of baggage.
     *
     * @param bagTag unique luggage tag
     * @param status the new BaggageStatus name
     * @return updated Baggage entity
     */
    @Transactional
    public Baggage updateBaggageStatus(String bagTag, String status) {
        Baggage baggage = baggageRepository.findByBagTag(bagTag)
                .orElseThrow(() -> new ResourceNotFoundException("Baggage record not found for tag: " + bagTag));

        BaggageStatus newStatus = BaggageStatus.valueOf(status.toUpperCase());
        baggage.setStatus(newStatus);
        log.info("Updated baggage tag: {} status to: {}", bagTag, newStatus);
        return baggageRepository.save(baggage);
    }

    /**
     * Retrieves baggage items belonging to a passenger.
     *
     * @param passengerId passenger database ID
     * @return list of Baggage entries
     */
    @Transactional(readOnly = true)
    public List<Baggage> getBaggageByPassenger(Long passengerId) {
        return baggageRepository.findByPassengerId(passengerId);
    }

    /**
     * Looks up a baggage record by barcode tag.
     *
     * @param bagTag unique tag string
     * @return matching Baggage details
     */
    @Transactional(readOnly = true)
    public Baggage getBaggageByTag(String bagTag) {
        return baggageRepository.findByBagTag(bagTag)
                .orElseThrow(() -> new ResourceNotFoundException("Baggage tag not found: " + bagTag));
    }
}
