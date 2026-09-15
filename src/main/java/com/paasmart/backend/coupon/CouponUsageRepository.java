package com.paasmart.backend.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponIdAndCustomerId(Long couponId, Long customerId);

    // Order cancel hone par uski coupon-usage record dhundh kar delete karna, taaki usage-limit dobara free ho jaaye.
    Optional<CouponUsage> findByOrderId(Long orderId);
}