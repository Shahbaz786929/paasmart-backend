package com.paasmart.backend.tenant;

import com.paasmart.backend.auth.User;
import com.paasmart.backend.tenant.dto.CreateTenantAdminRequest;
import com.paasmart.backend.tenant.dto.CreateTenantRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/super-admin/tenants")
public class SuperAdminTenantController {

    @Autowired private SuperAdminTenantService superAdminTenantService;

    @GetMapping
    public ResponseEntity<List<Tenant>> allTenants() {
        return ResponseEntity.ok(superAdminTenantService.getAllTenants());
    }

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody CreateTenantRequest req) {
        return ResponseEntity.ok(superAdminTenantService.createTenant(req));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<Tenant> suspendTenant(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminTenantService.suspendTenant(id));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Tenant> activateTenant(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminTenantService.activateTenant(id));
    }

    @PostMapping("/{id}/admin")
    public ResponseEntity<User> createTenantAdmin(@PathVariable Long id, @Valid @RequestBody CreateTenantAdminRequest req) {
        return ResponseEntity.ok(superAdminTenantService.createTenantAdmin(id, req));
    }
}