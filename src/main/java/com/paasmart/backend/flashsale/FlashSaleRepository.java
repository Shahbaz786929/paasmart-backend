package com.paasmart.backend.flashsale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {
    List<FlashSale> findByShopIdOrderByCreatedAtDesc(Long shopId);

    // List isliye: agar galti se do sale overlap ho jaayein to Optional 500 error deta tha
    @Query("SELECT f FROM FlashSale f WHERE f.productId = :productId AND f.active = true " +
            "AND f.startsAt <= :now AND f.endsAt >= :now ORDER BY f.discountPercent DESC")
    List<FlashSale> findActiveForProduct(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    // Ek product pe same time me do flash sale na bane
    @Query("SELECT COUNT(f) > 0 FROM FlashSale f WHERE f.productId = :productId AND f.active = true " +
            "AND f.startsAt < :endsAt AND f.endsAt > :startsAt")
    boolean existsOverlapping(@Param("productId") Long productId,
                              @Param("startsAt") LocalDateTime startsAt,
                              @Param("endsAt") LocalDateTime endsAt);

    @Query("SELECT f FROM FlashSale f WHERE f.tenantId = :tenantId AND f.active = true AND f.startsAt <= :now AND f.endsAt >= :now")
    List<FlashSale> findAllCurrentlyActive(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
}