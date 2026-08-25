package com.paasmart.backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTenantAdminRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    private String phone;

    public String getName() {return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
