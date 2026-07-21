package com.paasmart.backend.chat;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.chat.dto.ConversationSummary;
import com.paasmart.backend.chat.dto.SendMessageRequest;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.UnauthorizedException;
import com.paasmart.backend.notification.PushNotificationService;
import com.paasmart.backend.order.Order;
import com.paasmart.backend.order.OrderRepository;
import com.paasmart.backend.seller.Shop;
import com.paasmart.backend.seller.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired private ChatConversationRepository conversationRepository;
    @Autowired private ChatMessageRepository messageRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PushNotificationService pushNotificationService;

    // Order ke liye conversation dhoondo, na ho to bana do (dono taraf se call ho sakta hai)
    public ChatConversation getOrCreateConversation(Long requesterId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        boolean isCustomer = order.getCustomerId().equals(requesterId);
        boolean isSeller = shop.getSellerId().equals(requesterId);

        if (!isCustomer && !isSeller) {
            throw new UnauthorizedException("You are not part of this order");
        }

        return conversationRepository.findByOrderId(orderId).orElseGet(() -> {
            ChatConversation conv = new ChatConversation();
            conv.setOrderId(orderId);
            conv.setCustomerId(order.getCustomerId());
            conv.setSellerId(shop.getSellerId());
            return conversationRepository.save(conv);
        });
    }

    public ChatMessage sendMessage(Long senderId, Long orderId, SendMessageRequest req) {
        ChatConversation conversation = getOrCreateConversation(senderId, orderId);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setMessage(req.getMessage());
        message = messageRepository.save(message);

        conversation.setLastMessageAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);

        // Dusre party ko notification bhejo
        Long recipientId = conversation.getCustomerId().equals(senderId)
                ? conversation.getSellerId()
                : conversation.getCustomerId();

        try {
            User recipient = userRepository.findById(recipientId).orElse(null);
            User sender = userRepository.findById(senderId).orElse(null);
            if (recipient != null && recipient.getPushToken() != null) {
                pushNotificationService.send(
                        recipient.getPushToken(),
                        "New message from " + (sender != null ? sender.getName() : "someone"),
                        req.getMessage().length() > 100 ? req.getMessage().substring(0, 100) + "..." : req.getMessage()
                );
            }
        } catch (Exception e) {
            System.out.println("Chat notification failed: " + e.getMessage());
        }

        return message;
    }

    public List<ChatMessage> getMessages(Long requesterId, Long orderId) {
        ChatConversation conversation = getOrCreateConversation(requesterId, orderId);

        // Dusre ke bheje hue messages ko read mark karo
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        messages.stream()
                .filter(m -> !m.getSenderId().equals(requesterId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });

        return messages;
    }

    // Customer ki saari chat conversations (list screen)
    public List<ConversationSummary> getMyConversationsAsCustomer(Long customerId) {
        return conversationRepository.findByCustomerIdOrderByLastMessageAtDesc(customerId).stream()
                .map(conv -> buildSummary(conv, customerId, conv.getSellerId()))
                .collect(Collectors.toList());
    }

    // Seller ki saari chat conversations
    public List<ConversationSummary> getMyConversationsAsSeller(Long sellerId) {
        return conversationRepository.findBySellerIdOrderByLastMessageAtDesc(sellerId).stream()
                .map(conv -> buildSummary(conv, sellerId, conv.getCustomerId()))
                .collect(Collectors.toList());
    }

    private ConversationSummary buildSummary(ChatConversation conv, Long viewerId, Long otherPartyId) {
        User otherParty = userRepository.findById(otherPartyId).orElse(null);
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId());

        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).getMessage();
        long unread = messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(conv.getId(), viewerId);

        return new ConversationSummary(
                conv.getId(),
                conv.getOrderId(),
                otherParty != null ? otherParty.getName() : "Unknown",
                lastMessage,
                conv.getLastMessageAt(),
                unread
        );
    }
}