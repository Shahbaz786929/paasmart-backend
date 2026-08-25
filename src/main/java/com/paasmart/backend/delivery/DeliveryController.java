package com.paasmart.backend.delivery;

import com.paasmart.backend.delivery.dto.UpdateLocationRequest;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderService;
import com.paasmart.backend.order.dto.DeliveryOtpRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    @Autowired private OrderService orderService;
    @Autowired private DeliveryLocationService deliveryLocationService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Nearby unassigned ready-to-pickup orders (sirf apne tenant ke, aur sirf jab on-duty ho)
    @GetMapping("/orders/available")
    public ResponseEntity<List<Order>> availableOrders() {
        return ResponseEntity.ok(orderService.getAvailableOrdersForDelivery(currentUserId()));
    }

    // Delivery partner ka current online/offline status
    @GetMapping("/duty")
    public ResponseEntity<Map<String, Boolean>> getDuty() {
        return ResponseEntity.ok(Map.of("onDuty", Boolean.TRUE.equals(orderService.getDeliveryDuty(currentUserId()))));
    }

    // Online/offline toggle karna — server par persist hota hai
    @PutMapping("/duty")
    public ResponseEntity<Map<String, Boolean>> setDuty(@RequestBody Map<String, Boolean> body) {
        boolean onDuty = Boolean.TRUE.equals(body.get("onDuty"));
        orderService.setDeliveryDuty(currentUserId(), onDuty);
        return ResponseEntity.ok(Map.of("onDuty", onDuty));
    }

    // Order accept
    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<Order> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.acceptOrderForDelivery(currentUserId(), id));
    }

    // Single assigned order ka full detail (order + items) — order screen ke liye
    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> orderDetail(@PathVariable Long id) {
        Order order = orderService.getDeliveryOrderDetail(currentUserId(), id);
        return ResponseEntity.ok(Map.of(
                "order", order,
                "items", orderService.getOrderItems(id)
        ));
    }

    // Customer ka naam/phone — sirf apne assigned order ke liye
    @GetMapping("/orders/{id}/customer")
    public ResponseEntity<Map<String, String>> customerInfo(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getCustomerInfoForDelivery(currentUserId(), id));
    }

    // Seller/shop-owner ka naam + shop ka naam + phone
    @GetMapping("/orders/{id}/seller")
    public ResponseEntity<Map<String, String>> sellerInfo(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getSellerInfoForDelivery(currentUserId(), id));
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

    // Delivery boy app periodically apni current location bhejega (jab tak koi active delivery ho)
    @PutMapping("/location")
    public ResponseEntity<Void> updateLocation(@Valid @RequestBody UpdateLocationRequest req) {
        deliveryLocationService.updateLocation(currentUserId(), req.getLat(), req.getLng());
        return ResponseEntity.ok().build();
    }

    /*
     * ===== MASKED CALLING (Exotel) — ABHI USE NAHI HO RAHA =====
     * OrderService.java me bataye gaye steps follow karke, jab masked calling
     * chahiye ho tab ye 2 endpoints uncomment kar dena.
     *
    @PostMapping("/orders/{id}/call-customer")
    public ResponseEntity<Map<String, String>> callCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.callCustomerMasked(currentUserId(), id));
    }

    @PostMapping("/orders/{id}/call-seller")
    public ResponseEntity<Map<String, String>> callSeller(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.callSellerMasked(currentUserId(), id));
    }
    */
}