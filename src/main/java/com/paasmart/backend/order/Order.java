package com.paasmart.backend.order;

import com.paasmart.backend.address.Address;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private Long shopId;
    private Long deliveryBoyId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PLACED;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode = PaymentMode.COD;

    private BigDecimal totalAmount;
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;
    private String otp;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime deliveredAt;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    public enum Status { PLACED, CONFIRMED, PREPARING, READY_FOR_PICKUP, PICKED_UP, IN_TRANSIT, DELIVERED, COMPLETED, CANCELLED }
    public enum PaymentMode { COD, ONLINE }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getDeliveryBoyId() { return deliveryBoyId; }
    public void setDeliveryBoyId(Long deliveryBoyId) { this.deliveryBoyId = deliveryBoyId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public Double getDeliveryLat() { return deliveryLat; }
    public void setDeliveryLat(Double deliveryLat) { this.deliveryLat = deliveryLat; }
    public Double getDeliveryLng() { return deliveryLng; }
    public void setDeliveryLng(Double deliveryLng) { this.deliveryLng = deliveryLng; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}