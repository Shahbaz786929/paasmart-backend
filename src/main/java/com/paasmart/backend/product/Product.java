package com.paasmart.backend.product;

import com.paasmart.backend.wishlist.Wishlist;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long shopId;
    private String name;
    private String category;
    private String subCategory;
    private BigDecimal price;
    private Integer discountPercent = 0;
    private Integer stockQty = 0;
    private String images; // comma-separated URLs abhi ke liye
    private String description;
    private Boolean isAvailable = true;
    private Boolean tryOnEnabled = false;
    private String voiceDescriptionUrl;
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product")
    private List<Wishlist> wishlists;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
    public Integer getStockQty() { return stockQty; }
    public void setStockQty(Integer stockQty) { this.stockQty = stockQty; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public Boolean getTryOnEnabled() { return tryOnEnabled; }
    public void setTryOnEnabled(Boolean tryOnEnabled) { this.tryOnEnabled = tryOnEnabled; }
    public String getVoiceDescriptionUrl() { return voiceDescriptionUrl; }
    public void setVoiceDescriptionUrl(String voiceDescriptionUrl) { this.voiceDescriptionUrl = voiceDescriptionUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getAvailable() { return isAvailable; }
    public void setAvailable(Boolean available) {isAvailable = available; }
    public List<Wishlist> getWishlists() { return wishlists; }
    public void setWishlists(List<Wishlist> wishlists) { this.wishlists = wishlists; }
}