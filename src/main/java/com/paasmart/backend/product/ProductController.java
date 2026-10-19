package com.paasmart.backend.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired private ProductService productService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return com.paasmart.backend.common.PageUtil.slice(productService.getAllAvailableProducts(category), page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }
}