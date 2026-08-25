package com.paasmart.backend.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long referenceId;

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ORDER_UPDATE/WALLET/RETURN_REQUEST/CHAT_MESSAGE/WISHLIST — Customer app
    // NEW_ORDER/SHOP_STATUS/RETURN_REQUEST/CHAT_MESSAGE       — Seller app
    // Delivery/Admin apps: aage features banne par isi enum me naya value add karna
    public enum Type {
        ORDER_UPDATE, NEW_ORDER, SHOP_STATUS, RETURN_REQUEST,
        CHAT_MESSAGE, WISHLIST, WALLET, PROMOTION, GENERAL, NEW_DELIVERY
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    @JsonProperty("isRead")
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}