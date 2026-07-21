package com.paasmart.backend.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponIdAndCustomerId(Long couponId, Long customerId);
}