package com.paasmart.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {
    List<WebPushSubscription> findByUserId(Long userId);
    Optional<WebPushSubscription> findByEndpoint(String endpoint);
    void deleteByEndpoint(String endpoint);
}