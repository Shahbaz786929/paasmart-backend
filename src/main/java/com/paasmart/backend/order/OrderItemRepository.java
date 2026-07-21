package com.paasmart.backend.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    // ---- Seller analytics: top-selling products ----
    @Query(value = """
            SELECT oi.product_id, p.name, SUM(oi.quantity) as total_qty, SUM(oi.subtotal) as total_revenue
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN products p ON p.id = oi.product_id
            WHERE o.shop_id = :shopId AND o.status = 'COMPLETED'
            GROUP BY oi.product_id, p.name
            ORDER BY total_qty DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findTopProductsByShop(@Param("shopId") Long shopId);
}