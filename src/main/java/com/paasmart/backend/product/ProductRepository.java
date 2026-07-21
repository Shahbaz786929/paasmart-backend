package com.paasmart.backend.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopId(Long shopId);
    List<Product> findByIsAvailableTrue();
    List<Product> findByCategoryIgnoreCaseAndIsAvailableTrue(String category);
    List<Product> findByShopIdAndIsAvailableTrue(Long shopId);

    // Seller analytics
    long countByShopId(Long shopId);
    long countByShopIdAndStockQty(Long shopId, Integer stockQty);

    // Search (pg_trgm  fuzzy/partial match)
    @Query(value = """
            SELECT * FROM products
            WHERE is_available = true
              AND (name ILIKE CONCAT('%', :query, '%') OR description ILIKE CONCAT('%', :query, '%'))
            ORDER BY similarity(name, :query) DESC
            LIMIT 50
            """, nativeQuery = true)
    List<Product> searchProducts(@Param("query") String query);
}