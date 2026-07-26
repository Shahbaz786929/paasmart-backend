package com.paasmart.backend.flashsale;

import com.paasmart.backend.flashsale.dto.FlashSaleRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/flash-sales")
public class FlashSaleController {

    @Autowired private FlashSaleService flashSaleService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<FlashSale> create(@Valid @RequestBody FlashSaleRequest req) {
        return ResponseEntity.ok(flashSaleService.createFlashSale(currentUserId(), req));
    }

    @GetMapping
    public ResponseEntity<List<FlashSale>> myFlashSales() {
        return ResponseEntity.ok(flashSaleService.getMyFlashSales(currentUserId()));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<FlashSale> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(flashSaleService.deactivate(currentUserId(), id));
    }
}