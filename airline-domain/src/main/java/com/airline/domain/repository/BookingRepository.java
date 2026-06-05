package com.airline.domain.repository;

import com.airline.common.model.BookingStatus;
import com.airline.domain.entity.Booking;
import com.airline.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    List<Booking> findByUser(User user);
    List<Booking> findByFlightIdAndBookingStatus(Long flightId, BookingStatus status);
}
