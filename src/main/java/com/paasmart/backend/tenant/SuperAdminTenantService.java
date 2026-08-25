package com.paasmart.backend.tenant;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.tenant.dto.CreateTenantAdminRequest;
import com.paasmart.backend.tenant.dto.CreateTenantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class SuperAdminTenantService {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant createTenant(CreateTenantRequest req) {
        String slug = req.getSlug().trim().toLowerCase();
        if (tenantRepository.existsBySlug(slug)) {
            throw new BadRequestExceprion("A tenant with this slug already exists");
        }
        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName(req.getName().trim());
        return tenantRepository.save(tenant);
    }

    public Tenant suspendTenant(Long tenantId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setStatus(Tenant.Status.SUSPENDED);
        return tenantRepository.save(tenant);
    }

    public Tenant activateTenant(Long tenantId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        tenant.setStatus(Tenant.Status.ACTIVE);
        return tenantRepository.save(tenant);
    }

    // Creates the first TENANT_ADMIN (city partner) account for a tenant
    public User createTenantAdmin(Long tenantId, CreateTenantAdminRequest req) {
        Tenant tenant = getTenantOrThrow(tenantId);

        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestExceprion("This phone number is already registered");
        }

        User admin = new User();
        admin.setName(req.getName());
        admin.setPhone(req.getPhone());
        admin.setRole(User.Role.TENANT_ADMIN);
        admin.setTenant(tenant);
        admin.setReferralCode(generateUniqueReferralCode());

        return userRepository.save(admin);
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = "PM" + String.format("%04d", new Random().nextInt(10000));
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }

    private Tenant getTenantOrThrow(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }
}