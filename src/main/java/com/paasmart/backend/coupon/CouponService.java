package com.paasmart.backend.coupon;

import com.paasmart.backend.coupon.dto.CouponApplyResponse;
import com.paasmart.backend.coupon.dto.CouponRequest;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponService {

    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponUsageRepository couponUsageRepository;

    // ---- Admin: create/manage coupons ----

    public Coupon createCoupon(CouponRequest req) {
        if (couponRepository.findByCodeIgnoreCaseAndActiveTrue(req.getCode()).isPresent()) {
            throw new BadRequestExceprion("A coupon with this code already exists");
        }

        Coupon coupon = new Coupon();
        coupon.setCode(req.getCode().toUpperCase());
        coupon.setDescription(req.getDescription());
        coupon.setDiscountType(Coupon.DiscountType.valueOf(req.getDiscountType().toUpperCase()));
        coupon.setDiscountValue(req.getDiscountValue());
        coupon.setMaxDiscountAmount(req.getMaxDiscountAmount());
        coupon.setMinOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : BigDecimal.ZERO);
        coupon.setUsageLimitPerUser(req.getUsageLimitPerUser() != null ? req.getUsageLimitPerUser() : 1);
        coupon.setValidFrom(req.getValidFrom());
        coupon.setValidUntil(req.getValidUntil());

        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public List<Coupon> getActiveCoupons() {
        return couponRepository.findByActiveTrue();
    }

    public Coupon deactivateCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        coupon.setActive(false);
        return couponRepository.save(coupon);
    }

    // ---- Customer: apply coupon at checkout ----

    // Sirf validate + discount calculate karta hai, actual redeem checkout ke waqt hota hai
    public CouponApplyResponse validateAndCalculate(Long customerId, String code, BigDecimal orderAmount) {
        Coupon coupon = getValidCoupon(customerId, code, orderAmount);
        BigDecimal discount = calculateDiscount(coupon, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);
        return new CouponApplyResponse(coupon.getCode(), discount, finalAmount);
    }

    // Order place hone ke baad usage record karna (CheckoutService se call hoga)
    public BigDecimal redeemCoupon(Long customerId, String code, BigDecimal orderAmount, Long orderId) {
        Coupon coupon = getValidCoupon(customerId, code, orderAmount);
        BigDecimal discount = calculateDiscount(coupon, orderAmount);

        CouponUsage usage = new CouponUsage();
        usage.setCouponId(coupon.getId());
        usage.setCustomerId(customerId);
        usage.setOrderId(orderId);
        couponUsageRepository.save(usage);

        return discount;
    }

    private Coupon getValidCoupon(Long customerId, String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> new BadRequestExceprion("Invalid or inactive coupon code"));

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new BadRequestExceprion("This coupon is not active yet");
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            throw new BadRequestExceprion("This coupon has expired");
        }
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestExceprion("Minimum order amount for this coupon is ₹" + coupon.getMinOrderAmount());
        }

        long usedCount = couponUsageRepository.countByCouponIdAndCustomerId(coupon.getId(), customerId);
        if (usedCount >= coupon.getUsageLimitPerUser()) {
            throw new BadRequestExceprion("You have already used this coupon the maximum number of times");
        }

        return coupon;
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == Coupon.DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        } else {
            discount = orderAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }

        // Discount kabhi order amount se zyada nahi ho sakta
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }
}