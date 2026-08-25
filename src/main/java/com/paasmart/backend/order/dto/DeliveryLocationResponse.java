package com.paasmart.backend.order.dto;

import java.time.LocalDateTime;

public class DeliveryLocationResponse {

    private Double lat;
    private Double lng;
    private LocalDateTime updatedAt;
    private boolean available;

    public DeliveryLocationResponse(Double lat, Double lng, LocalDateTime updatedAt, boolean available) {
        this.lat = lat;
        this.lng = lng;
        this.updatedAt = updatedAt;
        this.available = available;
    }

    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isAvailable() { return available; }
}