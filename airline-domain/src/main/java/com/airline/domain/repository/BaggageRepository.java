package com.airline.domain.repository;

import com.airline.domain.entity.Baggage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BaggageRepository extends JpaRepository<Baggage, Long> {
    Optional<Baggage> findByBagTag(String bagTag);
    List<Baggage> findByPassengerId(Long passengerId);
}
