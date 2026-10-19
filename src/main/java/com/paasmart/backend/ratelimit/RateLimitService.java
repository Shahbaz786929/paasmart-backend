package com.paasmart.backend.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;

    @Autowired
    private RateLimitRepository rateLimitRepository;

    // ---- Phone OTP (pehle se) ----
    public boolean tryConsumeOtpRequest(String phone) {
        return tryConsume("otp-request:" + phone, MAX_ATTEMPTS);
    }

    public boolean tryConsumeOtpVerify(String phone) {
        return tryConsume("otp-verify:" + phone, MAX_ATTEMPTS);
    }

    // ---- Email OTP (naya) ----
    // Email pe naya OTP bhejne ki limit (spam rokne ke liye)
    public boolean tryConsumeEmailOtpRequest(String email) {
        return tryConsume("email-otp-request:" + email, MAX_ATTEMPTS);
    }

    // Register ke time email OTP verify karne ki limit
    public boolean tryConsumeEmailOtpVerify(String email) {
        return tryConsume("email-otp-verify:" + email, MAX_ATTEMPTS);
    }

    // Forgot-password ka OTP verify karne ki limit
    public boolean tryConsumePasswordReset(String email) {
        return tryConsume("pwd-reset:" + email, MAX_ATTEMPTS);
    }

    // Password se login: thoda zyada chhoot (galti se typo hoti hai)
    public boolean tryConsumeEmailLogin(String email) {
        return tryConsume("email-login:" + email, 10);
    }

    // Delivery OTP: ek order pe 15 minute me 5 koshish
    public boolean tryConsumeDeliveryOtp(Long orderId) {
        return tryConsume("delivery-otp:" + orderId, MAX_ATTEMPTS);
    }

    @Transactional
    public boolean tryConsume(String rateKey, int maxAttempts) {
        RateLimitEntry entry = rateLimitRepository.findByRateKey(rateKey)
                .orElseGet(() -> {
                    RateLimitEntry fresh = new RateLimitEntry();
                    fresh.setRateKey(rateKey);
                    fresh.setRequestCount(0);
                    fresh.setWindowStart(LocalDateTime.now());
                    return fresh;
                });

        LocalDateTime now = LocalDateTime.now();
        long minutesSinceWindowStart = ChronoUnit.MINUTES.between(entry.getWindowStart(), now);

        if (minutesSinceWindowStart >= WINDOW_MINUTES) {
            entry.setRequestCount(0);
            entry.setWindowStart(now);
        }

        if (entry.getRequestCount() >= maxAttempts) {
            rateLimitRepository.save(entry);
            return false;
        }

        entry.setRequestCount(entry.getRequestCount() + 1);
        rateLimitRepository.save(entry);
        return true;
    }
}