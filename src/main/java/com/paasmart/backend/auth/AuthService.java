package com.paasmart.backend.auth;

import com.paasmart.backend.auth.dto.AuthResponse;
import com.paasmart.backend.auth.dto.LoginRequest;
import com.paasmart.backend.auth.dto.OtpVerifyRequest;
import com.paasmart.backend.auth.dto.RegisterRequest;
import com.paasmart.backend.config.JwtUtil;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public String register(RegisterRequest req) {
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestExceprion("This Phone Number Already Register");
        }

        User user = new User();
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        user.setReferralCode(generateUniqueReferralCode());

        // Apply referralCode
        if (req.getReferralCode() != null && !req.getReferralCode().isBlank()) {
            userRepository.findByReferralCode(req.getReferralCode().trim().toUpperCase())
                    .ifPresent(referrer -> user.setReferredBy(referrer.getId()));
        }

        userRepository.save(user);
        return "Registration Successful! You can login with the help og OTP";
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = "PM" + String.format("%04d", new Random().nextInt(10000));
        } while (userRepository.findByReferralCode(code).isPresent());
        return code;
    }

    public String requestOtp(LoginRequest req) {
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Phone number Not Found"));

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        System.out.println("OTP for " + req.getPhone() + " is: " + otp);

        return "OPT  is send on console";
    }

    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(req.getOtp())) {
            throw new BadRequestExceprion("OTP is wrong");
        }
        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestExceprion("OTP is expired Please try again");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    public void savePushToken(long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        user.setPushToken(token);
        userRepository.save(user);
    }
}
