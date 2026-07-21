package com.paasmart.backend.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);
    boolean existsByProductIdAndOrderId(Long productId, Long orderId);
    Optional<ProductReview> findByProductIdAndOrderId(Long productId, Long orderId);
    List<ProductReview> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM ProductReview r WHERE r.productId = :productId")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    long countByProductId(Long productId);
    long countByProductIdAndRating(Long productId, Integer rating);
}