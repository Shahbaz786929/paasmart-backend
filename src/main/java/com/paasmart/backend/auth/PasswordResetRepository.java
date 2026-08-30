package com.paasmart.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}