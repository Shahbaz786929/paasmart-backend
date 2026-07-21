package com.paasmart.backend.seller;

import com.paasmart.backend.seller.dto.ShopRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller")
public class SellerController {

    @Autowired private ShopService shopService;
    @Autowired private com.paasmart.backend.product.ProductService productService;
    @Autowired private com.paasmart.backend.order.OrderService orderService;
    @Autowired private SellerDashboardService sellerDashboardService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<com.paasmart.backend.seller.dto.SellerDashboardResponse> dashboard() {
        return ResponseEntity.ok(sellerDashboardService.getDashboard(currentUserId()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerShop(@Valid @RequestBody ShopRegisterRequest req) {
        Shop shop = shopService.registerShop(currentUserId(), req);
        return ResponseEntity.ok(shop);
    }

    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@Valid @RequestBody com.paasmart.backend.product.dto.ProductRequest req) {
        return ResponseEntity.ok(productService.addProduct(currentUserId(), req));
    }

    @GetMapping("/products")
    public ResponseEntity<?> myProducts() {
        return ResponseEntity.ok(productService.getMyProducts(currentUserId()));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @Valid @RequestBody com.paasmart.backend.product.dto.ProductRequest req) {
        return ResponseEntity.ok(productService.updateProduct(currentUserId(), id, req));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(currentUserId(), id);
        return ResponseEntity.ok("Product is deleted");
    }

    @GetMapping("/orders")
    public ResponseEntity<?> shopOrders() {
        return ResponseEntity.ok(orderService.getShopOrders(currentUserId()));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody com.paasmart.backend.order.dto.OrderStatusUpdateRequest req) {
        return ResponseEntity.ok(orderService.updateStatus(currentUserId(), id, req.getStatus()));
    }

    @GetMapping("/orders/{id}/customer")
    public ResponseEntity<?> orderCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getCustomerBasicInfo(id, currentUserId()));
    }

    @PutMapping("/shop")
    public ResponseEntity<?> updateShop(@Valid @RequestBody ShopRegisterRequest req) {
        return ResponseEntity.ok(shopService.updateShop(currentUserId(), req));
    }

    @PutMapping("/shop/delivery-radius")
    public ResponseEntity<Shop> updateDeliveryRadius(@RequestParam Double radiusKm) {
        return ResponseEntity.ok(shopService.updateDeliveryRadius(currentUserId(), radiusKm));
    }
}