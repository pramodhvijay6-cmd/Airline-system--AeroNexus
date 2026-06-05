package com.airline.domain.repository;

import com.airline.domain.entity.Flight;
import com.airline.domain.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    Optional<Flight> findByFlightNumber(String flightNumber);

    @Query("SELECT f FROM Flight f WHERE f.route = :route AND f.departureTime BETWEEN :startTime AND :endTime AND f.status = 'SCHEDULED'")
    List<Flight> findAvailableFlights(
        @Param("route") Route route,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT f FROM Flight f JOIN f.route r WHERE r.origin IN :origins AND r.destination IN :destinations AND f.departureTime BETWEEN :startTime AND :endTime")
    List<Flight> searchFlights(
        @Param("origins") java.util.List<String> origins,
        @Param("destinations") java.util.List<String> destinations,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
