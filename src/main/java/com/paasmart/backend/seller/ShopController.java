package com.paasmart.backend.seller;

import com.paasmart.backend.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops")
public class ShopController {

    @Autowired private ShopService shopService;
    @Autowired private ProductRepository productRepository;

    // GET /api/v1/shops?category=CLOTHING&city=Indore&lat=22.71&lng=75.85
    @GetMapping
    public ResponseEntity<?> nearbyShops(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return ResponseEntity.ok(shopService.getNearbyShops(category, city, lat, lng));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> shopDetail(@PathVariable Long id) {
        return ResponseEntity.ok(shopService.getApprovedShopById(id));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<?> shopProducts(@PathVariable Long id) {
        // ensures shop exists & is approved before showing its catalogue
        shopService.getApprovedShopById(id);
        return ResponseEntity.ok(productRepository.findByShopIdAndIsAvailableTrue(id));
    }
}