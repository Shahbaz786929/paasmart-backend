package com.paasmart.backend.review;

import com.paasmart.backend.review.dto.ProductReviewSummary;
import com.paasmart.backend.review.dto.ReviewRequest;
import com.paasmart.backend.review.dto.SellerReplyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProductReviewController {

    @Autowired private ProductReviewService reviewService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Customer review submit karta hai (rating + text + optional photos)
    @PostMapping(value = "/api/products/{productId}/reviews", consumes = "multipart/form-data")
    public ResponseEntity<ProductReview> addReview(
            @PathVariable Long productId,
            @RequestParam Long orderId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String reviewText,
            @RequestParam(required = false) List<MultipartFile> photos) {

        ReviewRequest req = new ReviewRequest();
        req.setRating(rating);
        req.setReviewText(reviewText);

        return ResponseEntity.ok(reviewService.addReview(currentUserId(), productId, orderId, req, photos));
    }

    // Public — product page pe koi bhi reviews dekh sakta hai
    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ProductReviewSummary> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @GetMapping("/api/reviews/my")
    public ResponseEntity<List<ProductReview>> myReviews() {
        return ResponseEntity.ok(reviewService.getMyReviews(currentUserId()));
    }

    // Seller reply
    @PutMapping("/api/v1/seller/reviews/{reviewId}/reply")
    public ResponseEntity<ProductReview> reply(@PathVariable Long reviewId, @RequestBody SellerReplyRequest req) {
        return ResponseEntity.ok(reviewService.replyToReview(currentUserId(), reviewId, req));
    }

    @PostMapping("/api/reviews/{reviewId}/helpful")
    public ResponseEntity<ProductReview> markHelpful(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.markHelpful(reviewId));
    }
}