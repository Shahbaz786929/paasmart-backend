package com.paasmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {

    @NotBlank(message = "Google authorization code is required")
    private String code;

    // Jo bhi redirect URI frontend ne authorization request mein use ki thi —
    // Google ko token exchange ke waqt yeh EXACT match karni chahiye.
    @NotBlank(message = "redirectUri is required")
    private String redirectUri;

    // PKCE ka code verifier (Authorization Code + PKCE flow ka hissa) — Expo
    // ka expo-auth-session isse khud generate karta hai.
    private String codeVerifier;

    // Sirf pehli baar naya user sign up karte waqt chahiye.
    private String phone;

    // Kaunsi app se request aa rahi hai — "SELLER", "CUSTOMER", "DELIVERY", "ADMIN"
    // Isse aage chal ke ek hi backend endpoint har app ke liye reusable ban jaata hai.
    private String role;

    private String tenantSlug;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getCodeVerifier() { return codeVerifier; }
    public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTenantSlug() { return tenantSlug; }
    public void setTenantSlug(String tenantSlug) { this.tenantSlug = tenantSlug; }
}