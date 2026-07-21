package com.paasmart.backend.tryon;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TryOnHistoryRepository extends JpaRepository<TryOnHistory, Long> {
    List<TryOnHistory> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}