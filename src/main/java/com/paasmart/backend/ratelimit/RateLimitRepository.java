package com.paasmart.backend.ratelimit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateLimitRepository extends JpaRepository<RateLimitEntry, Long> {
    Optional<RateLimitEntry> findByRateKey(String rateKey);
}