package com.airline.service;

import com.airline.common.exception.BadRequestException;
import com.airline.common.exception.ResourceNotFoundException;
import com.airline.domain.entity.Coupon;
import com.airline.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * Validates if a coupon code can be applied to a specific purchase amount.
     *
     * @param code        the discount coupon code string
     * @param orderAmount the total booking price before discount
     * @return the Coupon entity if valid, otherwise throws Exception
     */
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));

        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Coupon has expired");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("Coupon usage limit reached");
        }

        if (orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BadRequestException("Order amount is below the minimum required value of " + coupon.getMinOrderValue());
        }

        return coupon;
    }
}
