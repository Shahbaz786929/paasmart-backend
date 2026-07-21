package com.paasmart.backend.chat;

import com.paasmart.backend.chat.dto.ConversationSummary;
import com.paasmart.backend.chat.dto.SendMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired private ChatService chatService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Order se juda chat kholo (ya banao) — dono role use kar sakte hain
    @PostMapping("/order/{orderId}/messages")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable Long orderId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(currentUserId(), orderId, request));
    }

    // Order ke saare messages, poll karke naye dekhne ke liye har 3-5 second
    @GetMapping("/order/{orderId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long orderId) {
        return ResponseEntity.ok(chatService.getMessages(currentUserId(), orderId));
    }

    // Customer ki chat list
    @GetMapping("/customer/conversations")
    public ResponseEntity<List<ConversationSummary>> customerConversations() {
        return ResponseEntity.ok(chatService.getMyConversationsAsCustomer(currentUserId()));
    }

    // Seller ki chat list
    @GetMapping("/seller/conversations")
    public ResponseEntity<List<ConversationSummary>> sellerConversations() {
        return ResponseEntity.ok(chatService.getMyConversationsAsSeller(currentUserId()));
    }
}