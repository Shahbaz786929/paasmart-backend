package com.paasmart.backend.tryon;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

// FREE placeholder provider — asli AI transformation nahi karta,
// bas poora try-on flow (upload -> processing -> result) test karne ke liye hai.
// Jab paise aane lagen, application.properties me tryon.provider=fashn kar dena.
@Component
@Primary
public class MockTryOnProvider implements TryOnProvider {

    @Override
    public String startJob(String userPhotoUrl, String clothImageUrl, String category) {
        // Real provider me yahan ek external job id milta, yahan hum
        // seedha userPhotoUrl ko hi "jobId" ki tarah use kar lete hain
        // taaki checkJobStatus() ko pata rahe result kya dikhana hai.
        return userPhotoUrl;
    }

    @Override
    public Map<String, Object> checkJobStatus(String jobId) {
        // Mock hamesha turant "complete" ho jaata hai — koi wait nahi karna padta
        return Map.of(
                "status", "completed",
                "resultUrl", jobId   // user ki apni photo hi result ki tarah dikhegi
        );
    }
}