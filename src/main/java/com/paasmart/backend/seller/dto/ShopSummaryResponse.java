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
    private Double distanceKm;
    private Double deliveryRadiusKm;
    private boolean canDeliver;

    public ShopSummaryResponse(Shop shop, Double distanceKm, boolean canDeliver) {
        this.id = shop.getId();
        this.shopName = shop.getShopName();
        this.category = shop.getCategory() != null ? shop.getCategory().name() : null;
        this.address = shop.getAddress();
        this.city = shop.getCity();
        this.latitude = shop.getLatitude();
        this.longitude = shop.getLongitude();
        this.storeSlug = shop.getStoreSlug();
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
    public Double getDistanceKm() { return distanceKm; }
    public Double getDeliveryRadiusKm() { return deliveryRadiusKm; }
    public boolean isCanDeliver() { return canDeliver; }
}