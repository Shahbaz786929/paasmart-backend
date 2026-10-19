package com.paasmart.backend.tenant;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.auth.UserRepository;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.tenant.dto.CreateTenantAdminRequest;
import com.paasmart.backend.tenant.dto.CreateTenantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Random;

@Service
public class SuperAdminTenantService {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

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

        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestExceprion("This email is already registered");
        }

        User admin = new User();
        admin.setName(req.getName());
        admin.setPhone(req.getPhone());
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(req.getPassword()));
        admin.setRole(User.Role.TENANT_ADMIN);
        admin.setTenant(tenant);
        admin.setReferralCode(generateUniqueReferralCode());

        return userRepository.save(admin);
    }

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom secureRandom = new SecureRandom();

    private String generateUniqueReferralCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("PM");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }

    private Tenant getTenantOrThrow(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }
}