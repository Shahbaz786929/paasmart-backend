package com.paasmart.backend.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Order cancel hone par pata karna ki us order ke liye wallet se kitna use hua tha, taaki refund kar sake.
    Optional<WalletTransaction> findByOrderIdAndType(Long orderId, WalletTransaction.Type type);
}