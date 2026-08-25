package com.paasmart.backend.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateTenantRequest {

    @NotBlank(message = "Slug is required")
    private String slug;

    @NotBlank(message = "Name is required")
    private String name;

    public String getSlug() { return slug; }
    public void setSlug(String slug) {this.slug = slug; }
    public String getName () { return name; }
    public void setName(String name) { this.name = name; }
}
