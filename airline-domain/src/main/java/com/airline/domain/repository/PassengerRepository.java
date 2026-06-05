package com.airline.domain.repository;

import com.airline.domain.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    List<Passenger> findByBookingId(Long bookingId);

    @Query("SELECT p.seatNumber FROM Passenger p WHERE p.booking.flight.id = :flightId AND p.booking.bookingStatus = 'CONFIRMED'")
    List<String> findOccupiedSeatsByFlightId(@Param("flightId") Long flightId);
}
