package com.paasmart.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    long countByConversationIdAndIsReadFalseAndSenderIdNot(Long conversationId, Long excludeSenderId);
}