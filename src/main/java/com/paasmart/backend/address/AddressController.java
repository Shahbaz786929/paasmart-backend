package com.paasmart.backend.address;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Autowired private AddressService addressService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<Address> addAddress(@RequestBody AddAddressRequest request) {
        return ResponseEntity.ok(addressService.addAddress(currentUserId(), request));
    }

    @GetMapping
    public ResponseEntity<List<Address>> getAddresses() {
        return ResponseEntity.ok(addressService.getAddresses(currentUserId()));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<Address> updateAddress(
            @PathVariable Long addressId,
            @RequestBody AddAddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(currentUserId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
        addressService.deleteAddress(currentUserId(), addressId);
        return ResponseEntity.ok("Address Deleted Successfully");
    }

    @PutMapping("/default/{addressId}")
    public ResponseEntity<Address> setDefaultAddress(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(currentUserId(), addressId));
    }
}