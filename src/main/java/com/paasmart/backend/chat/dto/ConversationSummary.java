package com.paasmart.backend.chat.dto;

import java.time.LocalDateTime;

public class ConversationSummary {

    private Long conversationId;
    private Long orderId;
    private String otherPartyName;   // customer ke liye seller ka naam, seller ke liye customer ka naam
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public ConversationSummary(Long conversationId, Long orderId, String otherPartyName,
                               String lastMessage, LocalDateTime lastMessageAt, long unreadCount) {
        this.conversationId = conversationId;
        this.orderId = orderId;
        this.otherPartyName = otherPartyName;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public Long getConversationId() { return conversationId; }
    public Long getOrderId() { return orderId; }
    public String getOtherPartyName() { return otherPartyName; }
    public String getLastMessage() { return lastMessage; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public long getUnreadCount() { return unreadCount; }
}