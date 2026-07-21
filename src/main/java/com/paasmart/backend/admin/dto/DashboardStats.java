package com.paasmart.backend.admin.dto;

import java.math.BigDecimal;

public class DashboardStats {

    private long totalUsers;
    private long totalCustomers;
    private long totalSellers;
    private long totalDeliveryBoys;
    private long pendingShops;
    private long approvedShops;
    private long totalOrders;
    private long ordersToday;
    private BigDecimal totalRevenue;

    // Getters and setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    public long getTotalSellers() { return totalSellers; }
    public void setTotalSellers(long totalSellers) { this.totalSellers = totalSellers; }
    public long getTotalDeliveryBoys() { return totalDeliveryBoys; }
    public void setTotalDeliveryBoys(long totalDeliveryBoys) { this.totalDeliveryBoys = totalDeliveryBoys; }
    public long getPendingShops() { return pendingShops; }
    public void setPendingShops(long pendingShops) { this.pendingShops = pendingShops; }
    public long getApprovedShops() { return approvedShops; }
    public void setApprovedShops(long approvedShops) { this.approvedShops = approvedShops; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getOrdersToday() { return ordersToday; }
    public void setOrdersToday(long ordersToday) { this.ordersToday = ordersToday; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}