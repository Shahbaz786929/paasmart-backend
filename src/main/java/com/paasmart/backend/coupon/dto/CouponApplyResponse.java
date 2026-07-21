package com.paasmart.backend.coupon.dto;

import java.math.BigDecimal;

public class CouponApplyResponse {

    private String code;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public CouponApplyResponse(String code, BigDecimal discountAmount, BigDecimal finalAmount) {
        this.code = code;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
}