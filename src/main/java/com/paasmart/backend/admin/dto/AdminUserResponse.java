package com.paasmart.backend.admin.dto;

import com.paasmart.backend.auth.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminUserResponse {
    private Long id;
    private String name;
    private String phone;
    private String role;
    private String status;
    private Long tenantId;
    private BigDecimal walletBalance;
    private LocalDateTime createdAt;

    public AdminUserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.role = user.getRole().name();
        this.status = user.getStatus().name();
        this.tenantId = user.getTenant().getId();
        this.walletBalance = user.getWalletBalance();
        this.createdAt = user.getCreatedAt();
        // Note: otpCode/otpExpiresAt intentionally NOT included -- never expose OTPs
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public Long getTenantId() { return tenantId; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}