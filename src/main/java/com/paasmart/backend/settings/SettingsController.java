package com.paasmart.backend.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class SettingsController {

    @Autowired private SettingsService settingsService;

    // Public — used by the customer app to preview delivery pricing before checkout.
    @GetMapping("/api/v1/settings/delivery-fee")
    public ResponseEntity<?> getDeliveryPricing() {
        return ResponseEntity.ok(Map.of(
                "baseFee", settingsService.getDeliveryBaseFee(),
                "perKmFee", settingsService.getDeliveryPerKmFee()
        ));
    }

    // Admin-only — TENANT_ADMIN updates their city's base fee.
    @PutMapping("/api/v1/admin/settings/delivery-fee")
    public ResponseEntity<?> updateBaseFee(@RequestBody Map<String, BigDecimal> body) {
        BigDecimal fee = settingsService.updateDeliveryBaseFee(body.get("baseFee"));
        return ResponseEntity.ok(Map.of("baseFee", fee));
    }

    // Admin-only — TENANT_ADMIN updates their city's per-km fee.
    @PutMapping("/api/v1/admin/settings/delivery-per-km-fee")
    public ResponseEntity<?> updatePerKmFee(@RequestBody Map<String, BigDecimal> body) {
        BigDecimal fee = settingsService.updateDeliveryPerKmFee(body.get("perKmFee"));
        return ResponseEntity.ok(Map.of("perKmFee", fee));
    }
}