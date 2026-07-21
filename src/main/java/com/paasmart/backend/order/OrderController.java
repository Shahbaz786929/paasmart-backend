package com.paasmart.backend.order;

import com.paasmart.backend.order.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired private OrderService orderService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.ok(orderService.placeOrder(currentUserId(), req));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myOrders() {
        return ResponseEntity.ok(orderService.getMyOrders(currentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrderById(id, currentUserId());
        return ResponseEntity.ok(java.util.Map.of(
                "order", order,
                "items", orderService.getOrderItems(id)
        ));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id, currentUserId());
        return ResponseEntity.ok("Order has been canceled.");
    }
}