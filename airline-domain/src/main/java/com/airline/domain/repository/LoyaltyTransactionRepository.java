package com.airline.domain.repository;

import com.airline.domain.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    List<LoyaltyTransaction> findByLoyaltyPointIdOrderByTransactionDateDesc(Long loyaltyPointId);
}
