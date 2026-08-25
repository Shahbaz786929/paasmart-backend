package com.paasmart.backend.tenant;

import com.paasmart.backend.tenant.dto.TenantResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    @Autowired private TenantRepository tenantRepository;

    // Public — app "select your city" screen se, login se pehle hi call hota hai
    @GetMapping
    public List<TenantResponse> listActiveTenants() {
        return tenantRepository.findByStatusOrderByNameAsc(Tenant.Status.ACTIVE)
                .stream()
                .map(TenantResponse::new)
                .collect(Collectors.toList());
    }
}