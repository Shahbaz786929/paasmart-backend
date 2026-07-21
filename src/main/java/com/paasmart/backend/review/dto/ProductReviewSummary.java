package com.paasmart.backend.review.dto;

import java.util.List;
import java.util.Map;

public class ProductReviewSummary {

    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> ratingBreakdown;   // {5: 10, 4: 3, 3: 1, 2: 0, 1: 0}
    private List<com.paasmart.backend.review.ProductReview> reviews;

    public ProductReviewSummary(double averageRating, long totalReviews,
                                Map<Integer, Long> ratingBreakdown,
                                List<com.paasmart.backend.review.ProductReview> reviews) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingBreakdown = ratingBreakdown;
        this.reviews = reviews;
    }

    public double getAverageRating() { return averageRating; }
    public long getTotalReviews() { return totalReviews; }
    public Map<Integer, Long> getRatingBreakdown() { return ratingBreakdown; }
    public List<com.paasmart.backend.review.ProductReview> getReviews() { return reviews; }
}