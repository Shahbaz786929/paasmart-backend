package com.paasmart.backend.coupon;

import com.paasmart.backend.coupon.dto.CouponApplyRequest;
import com.paasmart.backend.coupon.dto.CouponApplyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired private CouponService couponService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Sabhi active coupons dikhana (customer app me "Available Offers" screen)
    @GetMapping
    public ResponseEntity<List<Coupon>> activeCoupons() {
        return ResponseEntity.ok(couponService.getActiveCoupons());
    }

    // Cart page pe coupon code daal ke check karna (order place hone se pehle)
    @PostMapping("/apply")
    public ResponseEntity<CouponApplyResponse> applyCoupon(@RequestBody CouponApplyRequest request) {
        return ResponseEntity.ok(
                couponService.validateAndCalculate(currentUserId(), request.getCode(), request.getOrderAmount())
        );
    }
}