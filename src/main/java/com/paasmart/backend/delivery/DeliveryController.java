package com.paasmart.backend.delivery;

import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderService;
import com.paasmart.backend.order.dto.DeliveryOtpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    @Autowired private OrderService orderService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Nearby unassigned ready-to-pickup orders
    @GetMapping("/orders/available")
    public ResponseEntity<List<Order>> availableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrdersForDelivery());
    }

    // Order accept
    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<Order> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.acceptOrderForDelivery(currentUserId(), id));
    }

    // (active + past) deliveries
    @GetMapping("/orders/my")
    public ResponseEntity<List<Order>> myDeliveries() {
        return ResponseEntity.ok(orderService.getMyDeliveries(currentUserId()));
    }

    // product pickup confirm on shop
    @PutMapping("/orders/{id}/picked-up")
    public ResponseEntity<Order> markPickedUp(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markPickedUp(currentUserId(), id));
    }

    @PutMapping("/orders/{id}/in-transit")
    public ResponseEntity<Order> markInTransit(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markInTransit(currentUserId(), id));
    }

    // confirm order with help of otp
    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<Order> confirmDelivery(
            @PathVariable Long id,
            @RequestBody DeliveryOtpRequest request) {
        return ResponseEntity.ok(orderService.confirmDelivery(currentUserId(), id, request.getOtp()));
    }
}