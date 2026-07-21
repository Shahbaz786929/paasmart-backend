package com.paasmart.backend.rating;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class RatingController {


    @Autowired
    private RatingService ratingService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Customer rates a delivered order
    @PostMapping("/api/v1/orders/{id}/rating")
    public ResponseEntity<?> rateOrder(@PathVariable Long id, @Valid @RequestBody RatingRequest req) {
        return ResponseEntity.ok(ratingService.rateOrder(currentUserId(), id, req));
    }

    // Public - anyone browsing a shop can see its ratings
    @GetMapping("/api/v1/shops/{id}/ratings")
    public ResponseEntity<?> shopRatings(@PathVariable Long id) {
        return ResponseEntity.ok(ratingService.getShopRating(id));
    }
}
