package com.paasmart.backend.seller;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;
    private String shopName;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String documentsUrl;

    private Double deliveryRadiusKm = 5.0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String rejectionReason;
    private String storeSlug;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Category { CLOTHING, FOOD, GENERAL, MULTI }
    public enum Status { PENDING, APPROVED, REJECTED, SUSPENDED }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getDocumentsUrl() { return documentsUrl; }
    public void setDocumentsUrl(String documentsUrl) { this.documentsUrl = documentsUrl; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getStoreSlug() { return storeSlug; }
    public void setStoreSlug(String storeSlug) { this.storeSlug = storeSlug; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


    public Double getDeliveryRadiusKm() { return deliveryRadiusKm; }
    public void setDeliveryRadiusKm(Double deliveryRadiusKm) { this.deliveryRadiusKm = deliveryRadiusKm; }
}