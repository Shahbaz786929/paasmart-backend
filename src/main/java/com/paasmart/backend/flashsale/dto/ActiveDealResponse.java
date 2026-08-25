package com.paasmart.backend.flashsale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ActiveDealResponse {

    private Long flashSaleId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
    private BigDecimal dealPrice;
    private Integer discountPercent;
    private LocalDateTime endsAt;

    public ActiveDealResponse(Long flashSaleId, Long productId, String productName, String productImage,
                              BigDecimal originalPrice, BigDecimal dealPrice,
                              Integer discountPercent, LocalDateTime endsAt) {
        this.flashSaleId = flashSaleId;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.originalPrice = originalPrice;
        this.dealPrice = dealPrice;
        this.discountPercent = discountPercent;
        this.endsAt = endsAt;
    }

    public Long getFlashSaleId() { return flashSaleId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductImage() { return productImage; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public BigDecimal getDealPrice() { return dealPrice; }
    public Integer getDiscountPercent() { return discountPercent; }
    public LocalDateTime getEndsAt() { return endsAt; }
}