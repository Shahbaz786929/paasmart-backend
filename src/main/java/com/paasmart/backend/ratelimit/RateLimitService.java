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

    // Ek phone number 15 minute mein max 5 baar naya OTP maang sakta hai
    public boolean tryConsumeOtpRequest(String phone) {
        return tryConsume("otp-request:" + phone);
    }

    // Ek phone number 15 minute mein max 5 baar OTP verify try kar sakta hai
    public boolean tryConsumeOtpVerify(String phone) {
        return tryConsume("otp-verify:" + phone);
    }

    @Transactional
    protected boolean tryConsume(String rateKey) {
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

        // 15 minute puri ho gayi -- window reset karo
        if (minutesSinceWindowStart >= WINDOW_MINUTES) {
            entry.setRequestCount(0);
            entry.setWindowStart(now);
        }

        if (entry.getRequestCount() >= MAX_ATTEMPTS) {
            rateLimitRepository.save(entry); // window-reset hua ho to wo bhi save ho jaaye
            return false; // limit exceed
        }

        entry.setRequestCount(entry.getRequestCount() + 1);
        rateLimitRepository.save(entry);
        return true; // allowed
    }
}