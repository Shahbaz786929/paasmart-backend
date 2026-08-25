package com.paasmart.backend.customer;

import com.paasmart.backend.auth.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfileResponse {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private String role;
    private String referralCode;
    private BigDecimal walletBalance;
    private String profileImage;
    private LocalDateTime createdAt;

    public ProfileResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.phone = user.getPhone();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.referralCode = user.getReferralCode();
        this.walletBalance = user.getWalletBalance();
        this.profileImage = user.getProfileImage();
        this.createdAt = user.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getReferralCode() { return referralCode; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public String getProfileImage() { return profileImage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}