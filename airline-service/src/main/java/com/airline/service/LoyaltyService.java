package com.airline.service;

import com.airline.common.model.LoyaltyTier;
import com.airline.common.model.LoyaltyTransactionType;
import com.airline.domain.entity.LoyaltyPoint;
import com.airline.domain.entity.LoyaltyTransaction;
import com.airline.domain.entity.User;
import com.airline.domain.repository.LoyaltyPointRepository;
import com.airline.domain.repository.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    /**
     * Accrues points for a completed booking ($10 = 1 loyalty point).
     *
     * @param user the User confirming the booking
     * @param bookingAmount the total amount paid
     */
    @Transactional
    public void accruePoints(User user, BigDecimal bookingAmount) {
        int pointsToEarn = bookingAmount.divide(BigDecimal.valueOf(10), 0, java.math.RoundingMode.DOWN).intValue();
        if (pointsToEarn <= 0) return;

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByUserId(user.getId())
                .orElseGet(() -> loyaltyPointRepository.save(
                        LoyaltyPoint.builder().user(user).currentBalance(0).tier(LoyaltyTier.BRONZE).build()
                ));

        loyaltyPoint.setCurrentBalance(loyaltyPoint.getCurrentBalance() + pointsToEarn);
        updateTier(loyaltyPoint);
        loyaltyPointRepository.save(loyaltyPoint);

        loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                .loyaltyPoint(loyaltyPoint)
                .points(pointsToEarn)
                .type(LoyaltyTransactionType.EARNED)
                .build());

        log.info("Accrued {} loyalty points for user: {}. New balance: {}", pointsToEarn, user.getUsername(), loyaltyPoint.getCurrentBalance());
    }

    /**
     * Deducts points from a user's loyalty account.
     *
     * @param user the User redeeming points
     * @param pointsToRedeem number of points to spend
     */
    @Transactional
    public void redeemPoints(User user, int pointsToRedeem) {
        if (pointsToRedeem <= 0) return;

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No loyalty account found for user: " + user.getUsername()));

        if (loyaltyPoint.getCurrentBalance() < pointsToRedeem) {
            throw new IllegalArgumentException("Insufficient loyalty points balance");
        }

        loyaltyPoint.setCurrentBalance(loyaltyPoint.getCurrentBalance() - pointsToRedeem);
        updateTier(loyaltyPoint);
        loyaltyPointRepository.save(loyaltyPoint);

        loyaltyTransactionRepository.save(LoyaltyTransaction.builder()
                .loyaltyPoint(loyaltyPoint)
                .points(pointsToRedeem)
                .type(LoyaltyTransactionType.REDEEMED)
                .build());

        log.info("Redeemed {} loyalty points for user: {}. Remaining balance: {}", pointsToRedeem, user.getUsername(), loyaltyPoint.getCurrentBalance());
    }

    private void updateTier(LoyaltyPoint loyaltyPoint) {
        int balance = loyaltyPoint.getCurrentBalance();
        LoyaltyTier oldTier = loyaltyPoint.getTier();
        LoyaltyTier newTier = LoyaltyTier.BRONZE;

        if (balance >= 10000) {
            newTier = LoyaltyTier.PLATINUM;
        } else if (balance >= 5000) {
            newTier = LoyaltyTier.GOLD;
        } else if (balance >= 1000) {
            newTier = LoyaltyTier.SILVER;
        }

        if (oldTier != newTier) {
            loyaltyPoint.setTier(newTier);
            log.info("User {} upgraded from {} to {}", loyaltyPoint.getUser().getUsername(), oldTier, newTier);
        }
    }
}
