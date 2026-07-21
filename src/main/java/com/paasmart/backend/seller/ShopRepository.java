package com.paasmart.backend.seller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findBySellerId(Long sellerId);
    boolean existsBySellerId(long sellerId);
    boolean existsByStoreSlug(String storeSlug);

    List<Shop> findByStatus(Shop.Status status);
    List<Shop> findByStatusAndCityIgnoreCase(Shop.Status status, String city);
    List<Shop> findByStatusAndCategory(Shop.Status status, Shop.Category category);

    // Search
    @Query(value = """
            SELECT * FROM shops
            WHERE status = 'APPROVED'
              AND shop_name ILIKE CONCAT('%', :query, '%')
            ORDER BY similarity(shop_name, :query) DESC
            LIMIT 30
            """, nativeQuery = true)
    List<Shop> searchShops(@Param("query") String query);
}