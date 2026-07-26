package com.paasmart.backend.flashsale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {
    List<FlashSale> findByShopIdOrderByCreatedAtDesc(Long shopId);

    @Query("SELECT f FROM FlashSale f WHERE f.productId = :productId AND f.active = true " +
            "AND f.startsAt <= :now AND f.endsAt >= :now")
    Optional<FlashSale> findActiveForProduct(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM FlashSale f WHERE f.active = true AND f.startsAt <= :now AND f.endsAt >= :now")
    List<FlashSale> findAllCurrentlyActive(@Param("now") LocalDateTime now);
}