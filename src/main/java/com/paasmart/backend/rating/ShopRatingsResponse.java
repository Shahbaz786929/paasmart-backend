package com.paasmart.backend.rating;

import java.time.LocalDateTime;
import java.util.List;

public class ShopRatingsResponse {
    private final double averageRating;
    private final long totalReviews;
    private final List<Item> ratings;

    public ShopRatingsResponse(double averageRating, long totalReviews, List<Item> ratings) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratings = ratings;
    }

    public double getAverageRating() { return averageRating; }
    public long getTotalReviews() { return totalReviews; }
    public List<Item> getRatings() { return ratings; }

    public static class Item {
        private final Long id;
        private final String customerName;   // "Ravi K." (poora naam public nahi)
        private final Integer productRating;
        private final Integer deliveryRating;
        private final String comment;
        private final LocalDateTime createdAt;

        public Item(Long id, String customerName, Integer productRating, Integer deliveryRating,
                    String comment, LocalDateTime createdAt) {
            this.id = id;
            this.customerName = customerName;
            this.productRating = productRating;
            this.deliveryRating = deliveryRating;
            this.comment = comment;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getCustomerName() { return customerName; }
        public Integer getProductRating() { return productRating; }
        public Integer getDeliveryRating() { return deliveryRating; }
        public String getComment() { return comment; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}