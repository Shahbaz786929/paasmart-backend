package com.paasmart.backend.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.paasmart.backend.seller.Shop;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopId(Long shopId);

    // Checkout ke waqt row-level lock ke saath fetch karo, taaki 2 customers
    // ek saath same product order karke stock negative me na le jaa sakein
    // (pehle wale TOCTOU race-condition bug ka fix).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    List<Product> findByIsAvailableTrue();
    List<Product> findByCategoryIgnoreCaseAndIsAvailableTrue(String category);
    List<Product> findByShopIdAndIsAvailableTrue(Long shopId);

    // --- Tenant-scoped versions (customer/seller-facing code isko use kare) ---
    List<Product> findByIsAvailableTrueAndTenantId(Long tenantId);
    List<Product> findByCategoryIgnoreCaseAndIsAvailableTrueAndTenantId(String category, Long tenantId);
    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);

    // Seller analytics
    long countByShopId(Long shopId);
    long countByShopIdAndStockQty(Long shopId, Integer stockQty);

    // Search -- tenant-scoped, same pattern as ShopRepository.searchShops
    @Query(value = """
            SELECT * FROM products
            WHERE is_available = true
              AND tenant_id = :tenantId
                            AND shop_id IN (SELECT id FROM shops WHERE status = 'APPROVED')
              AND (name ILIKE CONCAT('%', :query, '%') OR description ILIKE CONCAT('%', :query, '%'))
            ORDER BY similarity(name, :query) DESC
            LIMIT 50
            """, nativeQuery = true)
    List<Product> searchProducts(@Param("query") String query, @Param("tenantId") Long tenantId);

    // Customer ko sirf available product, aur sirf APPROVED shop ke (suspended/pending shop ke nahi)
    @Query("select p from Product p where p.isAvailable = true and p.tenantId = :tenantId " +
            "and p.shopId in (select s.id from Shop s where s.status = :status) order by p.id desc")
    List<Product> findVisibleByTenant(@Param("tenantId") Long tenantId, @Param("status") Shop.Status status);

    @Query("select p from Product p where lower(p.category) = lower(:category) and p.isAvailable = true " +
            "and p.tenantId = :tenantId and p.shopId in (select s.id from Shop s where s.status = :status) order by p.id desc")
    List<Product> findVisibleByCategoryAndTenant(@Param("category") String category,
                                                 @Param("tenantId") Long tenantId,
                                                 @Param("status") Shop.Status status);
}