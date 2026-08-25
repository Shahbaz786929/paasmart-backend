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

    // --- Tenant-scoped versions (customer/seller-facing code isko use kare) ---
    List<Shop> findByStatusAndTenantId(Shop.Status status, Long tenantId);
    List<Shop> findByStatusAndCityIgnoreCaseAndTenantId(Shop.Status status, String city, Long tenantId);
    List<Shop> findByStatusAndCategoryAndTenantId(Shop.Status status, Shop.Category category, Long tenantId);
    Optional<Shop> findByIdAndTenantId(Long id, Long tenantId);
    List<Shop> findByTenantId(Long tenantId);

    // --- Unscoped versions (sirf ADMIN/platform-wide dashboard ke liye) ---
    List<Shop> findByStatus(Shop.Status status);
    List<Shop> findByStatusAndCityIgnoreCase(Shop.Status status, String city);
    List<Shop> findByStatusAndCategory(Shop.Status status, Shop.Category category);

    // Search -- ab tenant-scoped, results kabhi cross-city leak nahi honge
    @Query(value = """
        SELECT * FROM shops
        WHERE status = 'APPROVED'
          AND tenant_id = :tenantId
          AND shop_name ILIKE CONCAT('%', :query, '%')
        ORDER BY similarity(shop_name, :query) DESC
        LIMIT 30
        """, nativeQuery = true)
    List<Shop> searchShops(@Param("query") String query, @Param("tenantId") Long tenantId);
}