package com.paasmart.backend.seller.dto;

import java.math.BigDecimal;
import java.util.List;

public class SellerDashboardResponse {

    private long totalOrders;
    private long ordersToday;
    private long ordersThisWeek;
    private long ordersThisMonth;

    private long pendingOrders;      // PLACED + CONFIRMED + PREPARING
    private long completedOrders;

    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;

    private long totalProducts;
    private long outOfStockProducts;

    private double averageRating;

    private List<TopProduct> topProducts;

    public static class TopProduct {
        private Long productId;
        private String name;
        private Integer totalQuantitySold;
        private BigDecimal totalRevenue;

        public TopProduct(Long productId, String name, Integer totalQuantitySold, BigDecimal totalRevenue) {
            this.productId = productId;
            this.name = name;
            this.totalQuantitySold = totalQuantitySold;
            this.totalRevenue = totalRevenue;
        }

        public Long getProductId() { return productId; }
        public String getName() { return name; }
        public Integer getTotalQuantitySold() { return totalQuantitySold; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
    }

    // Getters and setters
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getOrdersToday() { return ordersToday; }
    public void setOrdersToday(long ordersToday) { this.ordersToday = ordersToday; }
    public long getOrdersThisWeek() { return ordersThisWeek; }
    public void setOrdersThisWeek(long ordersThisWeek) { this.ordersThisWeek = ordersThisWeek; }
    public long getOrdersThisMonth() { return ordersThisMonth; }
    public void setOrdersThisMonth(long ordersThisMonth) { this.ordersThisMonth = ordersThisMonth; }
    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getRevenueThisMonth() { return revenueThisMonth; }
    public void setRevenueThisMonth(BigDecimal revenueThisMonth) { this.revenueThisMonth = revenueThisMonth; }
    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
    public long getOutOfStockProducts() { return outOfStockProducts; }
    public void setOutOfStockProducts(long outOfStockProducts) { this.outOfStockProducts = outOfStockProducts; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public List<TopProduct> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProduct> topProducts) { this.topProducts = topProducts; }
}