package com.paasmart.backend.auth;

import com.paasmart.backend.auth.dto.*;
import com.paasmart.backend.config.JwtUtil;
import com.paasmart.backend.exception.BadRequestExceprion;
import com.paasmart.backend.exception.ResourceNotFoundException;
import com.paasmart.backend.exception.TooManyRequestsException;
import com.paasmart.backend.ratelimit.RateLimitService;
import com.paasmart.backend.tenant.Tenant;
import com.paasmart.backend.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

@Service
public class AuthService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailService emailService;

    public String register(RegisterRequest req) {
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestExceprion("This Phone Number Already Register");
        }

        User user = new User();
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        user.setReferralCode(generateUniqueReferralCode());

        String tenantSlug = (req.getTenantSlug() == null || req.getTenantSlug().isBlank())
                ? "default"
                : req.getTenantSlug().trim().toLowerCase();
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new BadRequestExceprion("Invalid city/tenant: " + tenantSlug));
        if (tenant.getStatus() == Tenant.Status.SUSPENDED) {
            throw new BadRequestExceprion("This city's service is currently unavailable");
        }
        user.setTenant(tenant);

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
        if (!rateLimitService.tryConsumeOtpRequest(req.getPhone())) {
            throw new TooManyRequestsException("Too many OTP requests. Please try again after 15 minutes.");
        }

        User user = userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Phone number Not Found"));

        String otp = String.format("%06d", secureRandom.nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        log.info("OTP for {} is: {}", req.getPhone(), otp);

        return "OPT  is send on console";
    }

    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        if (!rateLimitService.tryConsumeOtpVerify(req.getPhone())) {
            throw new TooManyRequestsException("Too many failed attempts. Please try again after 15 minutes.");
        }

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

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    public void savePushToken(long userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        user.setPushToken(token);
        userRepository.save(user);
    }

    public AuthResponse googleLogin(GoogleAuthRequest req) {
        // Step 1: Authorization code ko Google ke pass secret ke sath exchange karo,
        // taaki humein verified id_token mile. Client secret hamesha yahin, backend
        // mein hi use hota hai — kisi bhi frontend app mein kabhi nahi.
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("code", req.getCode());
        form.add("redirect_uri", req.getRedirectUri());
        form.add("grant_type", "authorization_code");
        if (req.getCodeVerifier() != null && !req.getCodeVerifier().isBlank()) {
            form.add("code_verifier", req.getCodeVerifier());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(form, headers);

        Map tokenResponse;
        try {
            tokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token", requestEntity, Map.class);
        } catch (Exception e) {
            throw new BadRequestExceprion("Google sign-in failed. Please try again.");
        }

        if (tokenResponse == null || tokenResponse.get("id_token") == null) {
            throw new BadRequestExceprion("Google sign-in failed. Please try again.");
        }

        String idToken = (String) tokenResponse.get("id_token");

        // Step 2: id_token ko verify karo (Google ne khud issue kiya hai humare
        // secret se, isliye ye already trusted hai — bas payload padh rahe hain)
        Map<String, Object> googlePayload;
        try {
            googlePayload = restTemplate.getForObject(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken, Map.class);
        } catch (Exception e) {
            throw new BadRequestExceprion("Invalid Google token");
        }

        if (googlePayload == null || googlePayload.get("email") == null) {
            throw new BadRequestExceprion("Invalid Google token");
        }

        String email = (String) googlePayload.get("email");
        String name = (String) googlePayload.getOrDefault("name", email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            if (req.getPhone() == null || req.getPhone().isBlank()) {
                throw new BadRequestExceprion("PHONE_REQUIRED");
            }
            if (userRepository.existsByPhone(req.getPhone())) {
                throw new BadRequestExceprion("This phone number is already registered");
            }

            String tenantSlug = (req.getTenantSlug() == null || req.getTenantSlug().isBlank())
                    ? "default"
                    : req.getTenantSlug().trim().toLowerCase();
            Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                    .orElseThrow(() -> new BadRequestExceprion("Invalid city/tenant: " + tenantSlug));

            user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPhone(req.getPhone());
            user.setRole(User.Role.valueOf((req.getRole() == null ? "SELLER" : req.getRole()).toUpperCase()));
            user.setTenant(tenant);
            user.setReferralCode(generateUniqueReferralCode());
            userRepository.save(user);
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Your account has been blocked. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    @Transactional
    public String emailRegisterInit(EmailRegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestExceprion("An account with this email already exists");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new BadRequestExceprion("This phone number is already registered");
        }

        // Purani, unverified request(s) isi email ki hon to hata do
        emailVerificationRepository.deleteByEmail(req.getEmail());

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        EmailVerification verification = new EmailVerification();
        verification.setEmail(req.getEmail());
        verification.setOtpCode(otp);
        verification.setName(req.getName());
        verification.setPhone(req.getPhone());
        verification.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        verification.setRole(req.getRole() == null ? "SELLER" : req.getRole().toUpperCase());
        verification.setTenantSlug(req.getTenantSlug());
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        emailVerificationRepository.save(verification);

        emailService.sendOtpEmail(req.getEmail(), otp);

        return "OTP sent to " + req.getEmail();
    }

    @Transactional
    public AuthResponse emailRegisterVerify(EmailVerifyOtpRequest req) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(req.getEmail())
                .orElseThrow(() -> new BadRequestExceprion("No pending verification found for this email"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestExceprion("This OTP has expired. Please register again.");
        }

        if (!verification.getOtpCode().equals(req.getOtp())) {
            throw new BadRequestExceprion("Invalid OTP");
        }

        String tenantSlug = (verification.getTenantSlug() == null || verification.getTenantSlug().isBlank())
                ? "default"
                : verification.getTenantSlug().trim().toLowerCase();
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new BadRequestExceprion("Invalid city/tenant: " + tenantSlug));

        User user = new User();
        user.setName(verification.getName());
        user.setEmail(verification.getEmail());
        user.setPassword(verification.getPasswordHash());
        user.setPhone(verification.getPhone());
        user.setRole(User.Role.valueOf(verification.getRole()));
        user.setTenant(tenant);
        user.setReferralCode(generateUniqueReferralCode());
        userRepository.save(user);

        emailVerificationRepository.deleteByEmail(req.getEmail());

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }

    public AuthResponse emailLogin(EmailLoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestExceprion("Invalid email or password"));

        if (user.getPassword() == null) {
            throw new BadRequestExceprion("This account was created with Google. Please use \"Continue with Google\" to log in.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadRequestExceprion("Invalid email or password");
        }

        if (user.getStatus() == User.Status.BANNED) {
            throw new BadRequestExceprion("Ypur account has been blocked. Please contact support.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getTenant().getId());
        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name());
    }
}