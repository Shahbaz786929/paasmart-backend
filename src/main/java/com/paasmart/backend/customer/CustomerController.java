package com.paasmart.backend.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired private CustomerService customerService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Path ki {customerId} ab ignore hoti hai: hamesha logged-in user ka hi profile milta/badalta hai.
    @GetMapping("/profile/{customerId}")
    public ResponseEntity<CustomerProfileResponse> getProfile(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getProfile(currentUserId()));
    }

    @PutMapping("/profile/{customerId}")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @PathVariable Long customerId,
            @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(currentUserId(), request));
    }
}