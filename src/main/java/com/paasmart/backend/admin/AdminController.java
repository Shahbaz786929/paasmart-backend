package com.paasmart.backend.admin;

import com.paasmart.backend.admin.dto.DashboardStats;
import com.paasmart.backend.admin.dto.ShopRejectRequest;
import com.paasmart.backend.auth.User;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.seller.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired private AdminService adminService;

    // ---- Shops ----
    @GetMapping("/shops/pending")
    public ResponseEntity<List<Shop>> pendingShops() {
        return ResponseEntity.ok(adminService.getPendingShops());
    }

    @GetMapping("/shops")
    public ResponseEntity<List<Shop>> allShops() {
        return ResponseEntity.ok(adminService.getAllShops());
    }

    @PutMapping("/shops/{id}/approve")
    public ResponseEntity<Shop> approveShop(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveShop(id));
    }

    @PutMapping("/shops/{id}/reject")
    public ResponseEntity<Shop> rejectShop(@PathVariable Long id, @RequestBody ShopRejectRequest request) {
        return ResponseEntity.ok(adminService.rejectShop(id, request));
    }

    @PutMapping("/shops/{id}/suspend")
    public ResponseEntity<Shop> suspendShop(@PathVariable Long id, @RequestBody ShopRejectRequest request) {
        return ResponseEntity.ok(adminService.suspendShop(id, request));
    }

    // ---- Users ----
    @GetMapping("/users")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<User> blockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.blockUser(id));
    }

    @PutMapping("/users/{id}/unblock")
    public ResponseEntity<User> unblockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unblockUser(id));
    }

    // ---- Orders ----
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> allOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    // ---- Dashboard ----
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> dashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }
}