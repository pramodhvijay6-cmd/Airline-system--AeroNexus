package com.airline.domain.repository;

import com.airline.domain.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByOriginAndDestination(String origin, String destination);
}
