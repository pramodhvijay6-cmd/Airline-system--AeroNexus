package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.domain.entity.Coupon;
import com.airline.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Coupon>> validateCoupon(
            @RequestParam("code") String code,
            @RequestParam("amount") BigDecimal amount,
            HttpServletRequest servletRequest) {
        Coupon coupon = couponService.validateCoupon(code, amount);
        return ResponseEntity.ok(ApiResponse.success("Coupon code is valid", coupon, servletRequest.getRequestURI()));
    }
}
