package com.paasmart.backend.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "Product ka naam zaroori hai")
    private String name;

    private String category;
    private String subCategory;

    @NotNull(message = "Price zaroori hai")
    @Positive(message = "Price 0 se zyada honi chahiye")
    private BigDecimal price;

    private Integer discountPercent = 0;
    private Integer stockQty = 0;
    private String images;
    private String description;
    private Boolean tryOnEnabled = false;
    private String voiceDescriptionUrl;

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
    public Boolean getTryOnEnabled() { return tryOnEnabled; }
    public void setTryOnEnabled(Boolean tryOnEnabled) { this.tryOnEnabled = tryOnEnabled; }
    public String getVoiceDescriptionUrl() { return voiceDescriptionUrl; }
    public void setVoiceDescriptionUrl(String voiceDescriptionUrl) { this.voiceDescriptionUrl = voiceDescriptionUrl; }
}