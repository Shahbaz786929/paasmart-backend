package com.paasmart.backend.review.dto;

import com.paasmart.backend.review.ProductReview;

import java.time.LocalDateTime;

public class SellerReviewResponse {
    private final Long id;
    private final Long productId;
    private final String productName;
    private final String customerName;
    private final Integer rating;
    private final String reviewText;
    private final String sellerReply;
    private final LocalDateTime sellerRepliedAt;
    private final Integer helpfulCount;
    private final LocalDateTime createdAt;

    public SellerReviewResponse(ProductReview r, String productName, String customerName) {
        this.id = r.getId();
        this.productId = r.getProductId();
        this.productName = productName;
        this.customerName = customerName;
        this.rating = r.getRating();
        this.reviewText = r.getReviewText();
        this.sellerReply = r.getSellerReply();
        this.sellerRepliedAt = r.getSellerRepliedAt();
        this.helpfulCount = r.getHelpfulCount();
        this.createdAt = r.getCreatedAt();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCustomerName() { return customerName; }
    public Integer getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public String getSellerReply() { return sellerReply; }
    public LocalDateTime getSellerRepliedAt() { return sellerRepliedAt; }
    public Integer getHelpfulCount() { return helpfulCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}