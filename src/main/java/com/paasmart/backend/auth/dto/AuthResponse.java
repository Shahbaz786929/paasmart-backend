package com.paasmart.backend.auth.dto;

public class AuthResponse {

    private String token;
    private Long userId;
    private String name;
    private String role;

    public AuthResponse(String token, Long userId, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getName () { return name; }
    public String getRole () { return role; }
}
