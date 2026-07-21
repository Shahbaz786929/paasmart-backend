package com.paasmart.backend.wishlist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    @Autowired private WishlistService wishlistService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<Wishlist> addToWishlist(@RequestBody AddWishlistRequest request) {
        return ResponseEntity.ok(wishlistService.addToWishlist(currentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<Wishlist>> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist(currentUserId()));
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<String> removeWishlist(@PathVariable Long wishlistId) {
        wishlistService.removeWishlist(currentUserId(), wishlistId);
        return ResponseEntity.ok("Wishlist Item Removed Successfully");
    }
}