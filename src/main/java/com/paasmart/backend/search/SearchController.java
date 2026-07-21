package com.paasmart.backend.search;

import com.paasmart.backend.product.Product;
import com.paasmart.backend.product.ProductRepository;
import com.paasmart.backend.seller.GroUtils;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import com.paasmart.backend.seller.dto.ShopSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;

    // GET /api/search?q=kurta&lat=22.71&lng=75.85
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(new SearchResponse(List.of(), List.of()));
        }

        List<Product> products = productRepository.searchProducts(q.trim());

        List<Shop> matchedShops = shopRepository.searchShops(q.trim());
        List<ShopSummaryResponse> shops = matchedShops.stream()
                .map(shop -> {
                    Double distance = GroUtils.distanceKmOrNull(shop.getLatitude(), shop.getLongitude(), lat, lng);
                    boolean canDeliver = distance == null || distance <= shop.getDeliveryRadiusKm();
                    return new ShopSummaryResponse(shop, distance, canDeliver);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new SearchResponse(shops, products));
    }
}