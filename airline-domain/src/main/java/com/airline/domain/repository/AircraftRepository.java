package com.airline.domain.repository;

import com.airline.domain.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByTailNumber(String tailNumber);
}
