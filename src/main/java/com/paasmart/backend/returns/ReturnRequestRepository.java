package com.paasmart.backend.returns;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    boolean existsByOrderItemIdAndStatusIn(Long orderItemId, List<ReturnRequest.Status> statuses);

    // Do seller ek saath approve na kar paayein (double refund rokne ke liye row lock)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReturnRequest r where r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") Long id);

    // Seller ki shop ke saare returns (naye pehle)
    @Query("SELECT r FROM ReturnRequest r WHERE r.orderId IN (SELECT o.id FROM Order o WHERE o.shopId = :shopId) ORDER BY r.createdAt DESC")
    List<ReturnRequest> findAllForShop(@Param("shopId") Long shopId);
}