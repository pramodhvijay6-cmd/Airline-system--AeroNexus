package com.airline.service;

import com.airline.common.exception.ResourceNotFoundException;
import com.airline.domain.entity.Passenger;
import com.airline.domain.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PassengerService {

    private final PassengerRepository passengerRepository;

    /**
     * Updates passport details for an existing passenger profile.
     *
     * @param id the passenger database ID
     * @param passportNumber the new passport number string
     * @return updated Passenger entity
     */
    @Transactional
    public Passenger updatePassportInfo(Long id, String passportNumber) {
        Passenger passenger = passengerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + id));
        passenger.setPassportNumber(passportNumber);
        log.info("Updated passport info for passenger: {}", id);
        return passengerRepository.save(passenger);
    }
}
