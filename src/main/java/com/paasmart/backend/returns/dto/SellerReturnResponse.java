package com.paasmart.backend.returns.dto;

import com.paasmart.backend.order.OrderItem;
import com.paasmart.backend.returns.ReturnRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SellerReturnResponse {
    private final Long id;
    private final Long orderId;
    private final Long orderItemId;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal itemSubtotal;   // max refund yahi ho sakta hai
    private final String customerName;
    private final String reason;
    private final String description;
    private final String images;
    private final String status;
    private final String rejectionReason;
    private final BigDecimal refundAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime resolvedAt;

    public SellerReturnResponse(ReturnRequest r, OrderItem item, String customerName , BigDecimal maxRefund) {
        this.id = r.getId();
        this.orderId = r.getOrderId();
        this.orderItemId = r.getOrderItemId();
        this.productName = item != null ? item.getProductName() : "Item";
        this.quantity = item != null ? item.getQuantity() : null;
        this.itemSubtotal = maxRefund;
        this.customerName = customerName;
        this.reason = r.getReason();
        this.description = r.getDescription();
        this.images = r.getImages();
        this.status = r.getStatus().name();
        this.rejectionReason = r.getRejectionReason();
        this.refundAmount = r.getRefundAmount();
        this.createdAt = r.getCreatedAt();
        this.resolvedAt = r.getResolvedAt();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getOrderItemId() { return orderItemId; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getItemSubtotal() { return itemSubtotal; }
    public String getCustomerName() { return customerName; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public String getImages() { return images; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}