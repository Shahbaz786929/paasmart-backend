package com.paasmart.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findByOrderId(Long orderId);
    List<ChatConversation> findByCustomerIdOrderByLastMessageAtDesc(Long customerId);
    List<ChatConversation> findBySellerIdOrderByLastMessageAtDesc(Long sellerId);
}