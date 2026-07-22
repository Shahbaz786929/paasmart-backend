package com.paasmart.backend.returns;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    boolean existsByOrderItemIdAndStatusIn(Long orderItemId, List<ReturnRequest.Status> statuses);
}