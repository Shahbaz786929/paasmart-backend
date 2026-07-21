package com.paasmart.backend.auth;

import com.paasmart.backend.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login-otp")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.requestOtp(req));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return ResponseEntity.ok(authService.verifyOtp(req));
    }

    @PutMapping("/push-token")
    public ResponseEntity<?> savePushToken(@RequestBody Map<String, String> body) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        authService.savePushToken(userId, body.get("pushToken"));
        return ResponseEntity.ok("saved");
    }
}