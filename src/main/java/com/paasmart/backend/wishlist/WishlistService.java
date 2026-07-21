package com.paasmart.backend.wishlist;

import java.util.List;

public interface WishlistService {
    Wishlist addToWishlist(Long customerId, AddWishlistRequest request);
    List<Wishlist> getWishlist(Long customerId);
    void removeWishlist(Long customerId, Long wishlistId);
}