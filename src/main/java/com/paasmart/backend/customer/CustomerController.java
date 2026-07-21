package com.paasmart.backend.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired private CustomerService customerService;

    @GetMapping("/profile/{customerId}")
    public ResponseEntity<CustomerProfileResponse> getProfile(
            @PathVariable Long customerId) {

        return ResponseEntity.ok(
                customerService.getProfile(customerId)
        );
    }

    @PutMapping("/profile/{customerId}")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @PathVariable Long customerId,
            @RequestBody UpdateCustomerRequest request) {

        return ResponseEntity.ok(
                customerService.updateProfile(customerId, request)
        );
    }
}