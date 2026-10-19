package com.paasmart.backend.seller.dto;

import com.paasmart.backend.seller.Shop;

public class ShopSummaryResponse {

    private Long id;
    private String shopName;
    private String category;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private String storeSlug;
    private String logoUrl;
    private Double distanceKm;
    private Double deliveryRadiusKm;
    private boolean canDeliver;
    private double averageRating = 0.0;
    private long totalReviews = 0;
    private int estimatedDeliveryMinMinutes = 25;
    private int estimatedDeliveryMaxMinutes = 40;

    public ShopSummaryResponse(Shop shop, Double distanceKm, boolean canDeliver) {
        this.id = shop.getId();
        this.shopName = shop.getShopName();
        this.category = shop.getCategory() != null ? shop.getCategory().name() : null;
        this.address = shop.getAddress();
        this.city = shop.getCity();
        this.latitude = shop.getLatitude();
        this.longitude = shop.getLongitude();
        this.storeSlug = shop.getStoreSlug();
        this.logoUrl = shop.getLogoUrl();
        this.distanceKm = distanceKm;
        this.deliveryRadiusKm = shop.getDeliveryRadiusKm();
        this.canDeliver = canDeliver;
    }

    public Long getId() { return id; }
    public String getShopName() { return shopName; }
    public String getCategory() { return category; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getStoreSlug() { return storeSlug; }
    public String getLogoUrl() { return logoUrl; }
    public Double getDistanceKm() { return distanceKm; }
    public Double getDeliveryRadiusKm() { return deliveryRadiusKm; }
    public boolean isCanDeliver() { return canDeliver; }
    public double getAverageRating() { return averageRating; }
    public long getTotalReviews() { return totalReviews; }
    public int getEstimatedDeliveryMinMinutes() { return estimatedDeliveryMinMinutes; }
    public int getEstimatedDeliveryMaxMinutes() { return estimatedDeliveryMaxMinutes; }

    public void setRatingStats(double averageRating, long totalReviews) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
    }

    public void setEstimatedDelivery(int minMinutes, int maxMinutes) {
        this.estimatedDeliveryMinMinutes = minMinutes;
        this.estimatedDeliveryMaxMinutes = maxMinutes;
    }
}