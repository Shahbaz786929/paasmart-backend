package com.paasmart.backend.cart;

import java.util.List;

public interface CartService {
    CartResponse addToCart(Long customerId, AddToCartRequest request);
    List<CartResponse> getCustomerCart(Long customerId);
    CartResponse updateQuantity(Long customerId, Long cartId, Integer quantity);
    void removeFromCart(Long customerId, Long cartId);
    void clearCart(Long customerId);
}