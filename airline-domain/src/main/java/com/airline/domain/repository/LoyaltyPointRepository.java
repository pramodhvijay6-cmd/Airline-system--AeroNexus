package com.airline.domain.repository;

import com.airline.domain.entity.LoyaltyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyPointRepository extends JpaRepository<LoyaltyPoint, Long> {
    Optional<LoyaltyPoint> findByUserId(Long userId);
}
