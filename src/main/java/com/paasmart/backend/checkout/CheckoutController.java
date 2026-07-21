package com.paasmart.backend.checkout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired private CheckoutService checkoutService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> placeOrder(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.placeOrder(currentUserId(), request));
    }
}