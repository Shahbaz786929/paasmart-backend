package com.paasmart.backend.cart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired private CartService cartService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(currentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<CartResponse>> getCart() {
        return ResponseEntity.ok(cartService.getCustomerCart(currentUserId()));
    }

    @PutMapping("/{cartId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable Long cartId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(currentUserId(), cartId, quantity));
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> removeItem(@PathVariable Long cartId) {
        cartService.removeFromCart(currentUserId(), cartId);
        return ResponseEntity.ok("Item Removed Successfully");
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart() {
        cartService.clearCart(currentUserId());
        return ResponseEntity.ok("Cart Cleared Successfully");
    }
}