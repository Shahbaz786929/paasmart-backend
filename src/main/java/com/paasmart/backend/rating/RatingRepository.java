package com.paasmart.backend.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsByOrderId(Long orderId);
    Optional<Rating> findByOrderId(Long orderId);
    List<Rating> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("SELECT COALESCE(AVG(r.productRating), 0) FROM Rating r WHERE r.sellerId = :sellerId")
    Double getAverageRatingBySeller(@Param("sellerId") Long sellerId);
}
