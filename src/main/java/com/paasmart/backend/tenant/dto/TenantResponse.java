package com.paasmart.backend.tenant.dto;

import com.paasmart.backend.tenant.Tenant;

public class TenantResponse {
    private Long id;
    private String slug;
    private String name;

    public TenantResponse(Tenant tenant) {
        this.id = tenant.getId();
        this.slug = tenant.getSlug();
        this.name = tenant.getName();
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
}