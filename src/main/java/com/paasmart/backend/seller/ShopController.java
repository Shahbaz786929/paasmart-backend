package com.paasmart.backend.seller;

import com.paasmart.backend.common.PageUtil;
import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return PageUtil.slice(shopService.getNearbyShops(category, city, lat, lng), page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> shopDetail(@PathVariable Long id) {
        return ResponseEntity.ok(shopService.getPublicShopDetail(id));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<?> shopProducts(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // ensures shop exists & is approved before showing its catalogue
        shopService.getApprovedShopById(id);
        List<Product> products = new ArrayList<>(productRepository.findByShopIdAndIsAvailableTrue(id));
        products.sort(Comparator.comparing(Product::getId).reversed());
        return PageUtil.slice(products, page, size);
    }
}