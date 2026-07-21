package com.paasmart.backend.checkout;

public interface CheckoutService {
    CheckoutResponse placeOrder(Long customerId, CheckoutRequest request);
}