package com.paasmart.backend.wishlist;

public class AddWishlistRequest {

    private Long productId;

    public AddWishlistRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}