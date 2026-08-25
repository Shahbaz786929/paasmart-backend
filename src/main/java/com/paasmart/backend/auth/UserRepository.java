package com.paasmart.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository  extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    long countByRole(User.Role role);
    Optional<User> findByReferralCode(String referralCode);
    Optional<User> findByEmail(String email);


    List<User> findByTenantId(Long tenantId);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndRole(Long tenantId, User.Role role);
    Optional<User> findByIdAndTenantId(Long id, Long tenantId);
    List<User> findByRoleAndTenant_IdAndOnDuty(User.Role role, Long tenantId, Boolean onDuty);
}
