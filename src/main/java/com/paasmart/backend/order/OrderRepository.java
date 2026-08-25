package com.paasmart.backend.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByShopIdOrderByCreatedAtDesc(Long shopId);

    List<Order> findByStatusAndDeliveryBoyIdIsNull(Order.Status status);
    List<Order> findByStatusAndDeliveryBoyIdIsNullAndTenantId(Order.Status status, Long tenantId);
    List<Order> findByDeliveryBoyIdOrderByCreatedAtDesc(Long deliveryBoyId);

    // ---- Admin dashboard ke liye ----
    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    java.math.BigDecimal getTotalRevenue();

    List<Order> findAllByOrderByCreatedAtDesc();

    // ---- Seller analytics ----
    List<Order> findByShopIdAndCreatedAtAfter(Long shopId, LocalDateTime dateTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.shopId = :shopId AND o.status = 'COMPLETED'")
    java.math.BigDecimal getShopTotalRevenue(@Param("shopId") Long shopId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.shopId = :shopId AND o.status = 'COMPLETED' AND o.createdAt >= :since")
    java.math.BigDecimal getShopRevenueSince(@Param("shopId") Long shopId, @Param("since") LocalDateTime since);

    long countByShopIdAndCreatedAtAfter(Long shopId, LocalDateTime dateTime);
    long countByShopId(Long shopId);
    long countByShopIdAndStatus(Long shopId, Order.Status status);

    // --- Tenant-scoped (future: TENANT_ADMIN apne city ka order-dashboard banayega isse) ---
    List<Order> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    long countByTenantIdAndCreatedAtAfter(Long tenantId, LocalDateTime dateTime);
    long countByTenantId(Long tenantId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED' AND o.tenantId = :tenantId")
    java.math.BigDecimal getTenantTotalRevenue(@Param("tenantId") Long tenantId);
}