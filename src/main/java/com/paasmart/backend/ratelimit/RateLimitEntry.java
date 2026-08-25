package com.paasmart.backend.ratelimit;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limits")
public class RateLimitEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "otp-request:9876543210" ya "otp-verify:9876543210"
    @Column(name = "rate_key", nullable = false, unique = true)
    private String rateKey;

    private int requestCount;

    private LocalDateTime windowStart;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRateKey() { return rateKey; }
    public void setRateKey(String rateKey) { this.rateKey = rateKey; }
    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }
    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }
}