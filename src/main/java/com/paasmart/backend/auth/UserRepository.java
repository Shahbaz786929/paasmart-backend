package com.paasmart.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
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

    // Row-level lock for wallet credit/debit, so two simultaneous wallet operations
    // on the same user can't cause a lost-update on walletBalance.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}