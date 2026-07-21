package com.paasmart.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Phone Number is required")
    private String phone;

    public String getPhone() {return phone; }
    public void setPhone(String phone) {this.phone = phone; }
}
